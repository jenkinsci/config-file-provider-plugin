package org.jenkinsci.plugins.configfiles.maven.job;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserIdCause;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.server.ServerCredentialMapping;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MvnSettingsCredentialsTest {

    @Rule
    public JenkinsRule                        j = new JenkinsRule();

    @Inject
    private MavenSettingsConfigProvider       mavenSettingProvider;

    @Inject
    private GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    public void serverCredentialsMustBeInSettingsXmlAtRuntime() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createProject(MavenModuleSet.class);

        p.setMaven(ToolInstallations.configureMaven35().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize");

        final MavenSettingsConfig settings = createSettings(mavenSettingProvider);
        final MvnSettingsProvider mvnSettingsProvider = new MvnSettingsProvider(settings.id);
        DelegatingMvnSettingsProvider delegator = new DelegatingMvnSettingsProvider(mvnSettingsProvider);

        final GlobalMavenSettingsConfig globalSettings = createGlobalSettings(globalMavenSettingsConfigProvider);
        final MvnGlobalSettingsProvider mvnGlobalSettingsProvider = new MvnGlobalSettingsProvider(globalSettings.id);
        DelegatingGlobalMvnSettingsProvider delegator2 = new DelegatingGlobalMvnSettingsProvider(mvnGlobalSettingsProvider);

        p.setSettings(delegator);
        p.setGlobalSettings(delegator2);

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    private static final class DelegatingMvnSettingsProvider extends MvnSettingsProvider {

        private final MvnSettingsProvider mvnSettingsProvider;

        public DelegatingMvnSettingsProvider(MvnSettingsProvider mvnSettingsProvider) {
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
            Assert.assertTrue("settings must contain username", settingContent.contains("<username>foo</username>"));
            Assert.assertTrue("settings must contain password", settingContent.contains("<password>bar</password>"));
            return settingsPath;
        }
    }

    private static final class DelegatingGlobalMvnSettingsProvider extends MvnGlobalSettingsProvider {

        private final MvnGlobalSettingsProvider mvnSettingsProvider;

        public DelegatingGlobalMvnSettingsProvider(MvnGlobalSettingsProvider mvnSettingsProvider) {
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
            Assert.assertTrue("settings must contain username", settingContent.contains("<username>dude</username>"));
            Assert.assertTrue("settings must contain password", settingContent.contains("<password>dudepwd</password>"));
            return settingsPath;
        }
    }

    private MavenSettingsConfig createSettings(MavenSettingsConfigProvider provider) throws Exception {

        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "credid", "dummy desc", "foo", "bar"));

        ServerCredentialMapping mapping = new ServerCredentialMapping("myserver", "credid");
        List<ServerCredentialMapping> serverCredentialMappings = new ArrayList<ServerCredentialMapping>();
        serverCredentialMappings.add(mapping);

        MavenSettingsConfig c1 = (MavenSettingsConfig) provider.newConfig();
        MavenSettingsConfig c2 = new MavenSettingsConfig(c1.id + "dummy", c1.name, c1.comment, c1.content, MavenSettingsConfig.isReplaceAllDefault, serverCredentialMappings, null);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c2);

        return c2;
    }

    private GlobalMavenSettingsConfig createGlobalSettings(GlobalMavenSettingsConfigProvider provider) throws Exception {

        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "dudecredid", "dummy desc", "dude", "dudepwd"));

        ServerCredentialMapping mapping = new ServerCredentialMapping("someserver", "dudecredid");
        List<ServerCredentialMapping> serverCredentialMappings = new ArrayList<ServerCredentialMapping>();
        serverCredentialMappings.add(mapping);

        GlobalMavenSettingsConfig c1 = (GlobalMavenSettingsConfig) provider.newConfig();
        GlobalMavenSettingsConfig c2 = new GlobalMavenSettingsConfig(c1.id + "dummy2", c1.name, c1.comment, c1.content,  GlobalMavenSettingsConfig.isReplaceAllDefault, serverCredentialMappings, null);

        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c2);

        return c2;
    }
}
