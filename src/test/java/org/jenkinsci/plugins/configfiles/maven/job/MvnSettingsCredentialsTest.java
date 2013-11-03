package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserCause;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class MvnSettingsCredentialsTest {

    @Rule
    public JenkinsRule                  j = new JenkinsRule();

    @Inject
    private MavenSettingsConfigProvider mavenSettingProvider;

    @Test
    public void serverCredentialsMustBeInSettingsXmlAtRuntime() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createMavenProject("mvn");

        p.setMaven(j.configureMaven3().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/org/jenkinsci/plugins/configfiles/maven3-project.zip")));
        p.setGoals("initialize");

        final MavenSettingsConfig settings = createSetting(mavenSettingProvider);
        final MvnSettingsProvider mvnSettingsProvider = new MvnSettingsProvider(settings.id);
        DelegatingMvnSettingsProvider delegater = new DelegatingMvnSettingsProvider(mvnSettingsProvider);
        p.setSettings(delegater);

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
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

    private MavenSettingsConfig createSetting(MavenSettingsConfigProvider provider) throws Exception {

        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "credid", "dummy desc", "foo", "bar"));

        ServerCredentialMapping mapping = new ServerCredentialMapping("myserver", "credid");
        List<ServerCredentialMapping> mappings = new ArrayList<ServerCredentialMapping>();
        mappings.add(mapping);

        MavenSettingsConfig c1 = (MavenSettingsConfig) provider.newConfig();
        MavenSettingsConfig c2 = new MavenSettingsConfig(c1.id + "dummy", c1.name, c1.comment, c1.content, mappings);
        provider.save(c2);
        return c2;
    }
}
