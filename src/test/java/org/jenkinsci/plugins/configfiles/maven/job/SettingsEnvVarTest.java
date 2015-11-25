package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserCause;
import hudson.tasks.Builder;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

public class SettingsEnvVarTest {
    @Rule
    public JenkinsRule                        j = new JenkinsRule();

    @Inject
    private MavenSettingsConfigProvider       mavenSettingProvider;

    @Inject
    private GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    public void serverCredentialsMustBeInSettingsXmlAtRuntime() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createMavenProject("mvn");

        p.setMaven(j.configureMaven3().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize");

        final MavenSettingsConfig settings = createSettings(mavenSettingProvider);
        final MvnSettingsProvider mvnSettingsProvider = new MvnSettingsProvider(settings.id);

        final GlobalMavenSettingsConfig globalSettings = createGlobalSettings(globalMavenSettingsConfigProvider);
        final MvnGlobalSettingsProvider mvnGlobalSettingsProvider = new MvnGlobalSettingsProvider(globalSettings.id);

        p.setSettings(mvnSettingsProvider);
        p.setGlobalSettings(mvnGlobalSettingsProvider);

        p.getPostbuilders().add(new VerifyBuilder());

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }

    private static final class VerifyBuilder extends Builder {

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            try {
                final String userSettings = TokenMacro.expandAll(build, listener, "${ENV, var=\"MVN_SETTINGS\"}");
                final String globalSettings = TokenMacro.expandAll(build, listener, "${ENV, var=\"MVN_GLOBALSETTINGS\"}");
                System.out.println("-->" + userSettings);
                System.out.println("-->" + globalSettings);
                Assert.assertTrue("env variable for user settings is not set", StringUtils.isNotBlank(userSettings));
                Assert.assertTrue("env variable for global settings is not set", StringUtils.isNotBlank(globalSettings));
            } catch (MacroEvaluationException e) {
                Assert.fail("not able to expand var: " + e.getMessage());
            }
            return true;
        }
    }

    private MavenSettingsConfig createSettings(MavenSettingsConfigProvider provider) throws Exception {

        MavenSettingsConfig c1 = (MavenSettingsConfig) provider.newConfig(String.valueOf(System.currentTimeMillis()));
        provider.save(c1);
        return c1;
    }

    private GlobalMavenSettingsConfig createGlobalSettings(GlobalMavenSettingsConfigProvider provider) throws Exception {

        GlobalMavenSettingsConfig c1 = (GlobalMavenSettingsConfig) provider.newConfig(String.valueOf(System.currentTimeMillis()));
        provider.save(c1);
        return c1;
    }
}
