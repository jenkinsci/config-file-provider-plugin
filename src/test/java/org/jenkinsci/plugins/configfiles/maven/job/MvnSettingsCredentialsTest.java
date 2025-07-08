package org.jenkinsci.plugins.configfiles.maven.job;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserIdCause;
import hudson.model.Result;
import hudson.model.TaskListener;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MvnSettingsCredentialsTest {

    @Inject
    private MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    private GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    @Issue("SECURITY-3090")
    void serverCredentialsMustBeInSettingsXmlAtRuntime(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createProject(MavenModuleSet.class);

        p.setMaven(ToolInstallations.configureMaven35().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize");

        final MavenSettingsConfig settings = createSettings(j, mavenSettingProvider);
        final MvnSettingsProvider mvnSettingsProvider = new MvnSettingsProvider(settings.id);
        DelegatingMvnSettingsProvider delegator = new DelegatingMvnSettingsProvider(mvnSettingsProvider);

        final GlobalMavenSettingsConfig globalSettings = createGlobalSettings(j, globalMavenSettingsConfigProvider);
        final MvnGlobalSettingsProvider mvnGlobalSettingsProvider = new MvnGlobalSettingsProvider(globalSettings.id);
        DelegatingGlobalMvnSettingsProvider delegator2 =
                new DelegatingGlobalMvnSettingsProvider(mvnGlobalSettingsProvider);

        p.setSettings(delegator);
        p.setGlobalSettings(delegator2);

        // SECURITY-3090
        p.getPostbuilders().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                EnvVars environment = build.getEnvironment(listener);
                String settingsPath = environment.get("MVN_SETTINGS");
                assertNotNull(settingsPath, "There should be an environment var specifying the mvn settings");
                String globalSettingsPath = environment.get("MVN_GLOBALSETTINGS");
                assertNotNull(
                        globalSettingsPath, "There should be an environment var specifying the mvn global settings");
                String settings = build.getWorkspace().child(settingsPath).readToString();
                listener.getLogger().println("SETTINGS:\n" + settings);
                String globalSettings =
                        build.getWorkspace().child(globalSettingsPath).readToString();
                listener.getLogger().println("GLOBAL SETTINGS:\n" + globalSettings);
                return true;
            }
        });

        MavenModuleSetBuild build = j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());

        j.assertLogNotContains("<password>bar</password>", build);
        j.assertLogNotContains("<username>foo</username>", build);
        j.assertLogNotContains("<password>dudepwd</password>", build);
        j.assertLogNotContains("<username>dude</username>", build);
        j.assertLogContains("<username>****</username>", build);
        j.assertLogContains("<password>****</password>", build);
        if (Functions.isWindows()) {
            // Wait before exiting the test so that files close before cleanup
            Thread.sleep(3011);
        }
    }

    private static final class DelegatingMvnSettingsProvider extends MvnSettingsProvider {

        private final MvnSettingsProvider mvnSettingsProvider;

        public DelegatingMvnSettingsProvider(MvnSettingsProvider mvnSettingsProvider) {
            super(mvnSettingsProvider.getSettingsConfigId());
            this.mvnSettingsProvider = mvnSettingsProvider;
        }

        @Override
        public FilePath supplySettings(AbstractBuild<?, ?> build, TaskListener listener) {
            final FilePath settingsPath = mvnSettingsProvider.supplySettings(build, listener);
            String settingContent = "N/A";
            try {
                settingContent = FileUtils.readFileToString(new File(settingsPath.getRemote()), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertTrue(settingContent.contains("<username>foo</username>"), "settings must contain username");
            assertTrue(settingContent.contains("<password>bar</password>"), "settings must contain password");
            return settingsPath;
        }
    }

    private static final class DelegatingGlobalMvnSettingsProvider extends MvnGlobalSettingsProvider {

        private final MvnGlobalSettingsProvider mvnSettingsProvider;

        public DelegatingGlobalMvnSettingsProvider(MvnGlobalSettingsProvider mvnSettingsProvider) {
            super(mvnSettingsProvider.getSettingsConfigId());
            this.mvnSettingsProvider = mvnSettingsProvider;
        }

        @Override
        public FilePath supplySettings(AbstractBuild<?, ?> build, TaskListener listener) {
            final FilePath settingsPath = mvnSettingsProvider.supplySettings(build, listener);
            String settingContent = "N/A";
            try {
                settingContent = FileUtils.readFileToString(new File(settingsPath.getRemote()), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertTrue(settingContent.contains("<username>dude</username>"), "settings must contain username");
            assertTrue(settingContent.contains("<password>dudepwd</password>"), "settings must contain password");
            return settingsPath;
        }
    }

    private MavenSettingsConfig createSettings(JenkinsRule j, MavenSettingsConfigProvider provider) throws Exception {

        CredentialsStore store =
                CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        UsernamePasswordCredentialsImpl credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "credid", "dummy desc", "foo", "bar");
        credentials.setUsernameSecret(true);
        store.addCredentials(Domain.global(), credentials);

        ServerCredentialMapping mapping = new ServerCredentialMapping("myserver", "credid");
        List<ServerCredentialMapping> mappings = new ArrayList<>();
        mappings.add(mapping);

        MavenSettingsConfig c1 = (MavenSettingsConfig) provider.newConfig();
        MavenSettingsConfig c2 = new MavenSettingsConfig(
                c1.id + "dummy", c1.name, c1.comment, c1.content, MavenSettingsConfig.isReplaceAllDefault, mappings);

        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c2);

        return c2;
    }

    private GlobalMavenSettingsConfig createGlobalSettings(JenkinsRule j, GlobalMavenSettingsConfigProvider provider)
            throws Exception {

        CredentialsStore store =
                CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "dudecredid", "dummy desc", "dude", "dudepwd");
        credentials.setUsernameSecret(true);
        store.addCredentials(Domain.global(), credentials);

        ServerCredentialMapping mapping = new ServerCredentialMapping("someserver", "dudecredid");
        List<ServerCredentialMapping> mappings = new ArrayList<>();
        mappings.add(mapping);

        GlobalMavenSettingsConfig c1 = (GlobalMavenSettingsConfig) provider.newConfig();
        GlobalMavenSettingsConfig c2 = new GlobalMavenSettingsConfig(
                c1.id + "dummy2",
                c1.name,
                c1.comment,
                c1.content,
                GlobalMavenSettingsConfig.isReplaceAllDefault,
                mappings);

        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c2);

        return c2;
    }
}
