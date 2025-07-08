package org.jenkinsci.plugins.configfiles.maven.job;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import hudson.Functions;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserIdCause;
import hudson.model.Result;
import hudson.tasks.Builder;
import jakarta.inject.Inject;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SettingsEnvVarTest {
    @Inject
    private MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    private GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    void serverCredentialsMustBeInSettingsXmlAtRuntime(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createProject(MavenModuleSet.class);

        p.setMaven(ToolInstallations.configureMaven35().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize");

        final MavenSettingsConfig settings = createSettings(j, mavenSettingProvider);
        final MvnSettingsProvider mvnSettingsProvider = new MvnSettingsProvider(settings.id);

        final GlobalMavenSettingsConfig globalSettings = createGlobalSettings(j, globalMavenSettingsConfigProvider);
        final MvnGlobalSettingsProvider mvnGlobalSettingsProvider = new MvnGlobalSettingsProvider(globalSettings.id);

        p.setSettings(mvnSettingsProvider);
        p.setGlobalSettings(mvnGlobalSettingsProvider);

        p.getPostbuilders().add(new VerifyBuilder());

        j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
        if (Functions.isWindows()) {
            // Wait before exiting the test so that files close before cleanup
            Thread.sleep(3011);
        }
    }

    private static final class VerifyBuilder extends Builder {

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            try {
                final String userSettings = TokenMacro.expandAll(build, listener, "${ENV, var=\"MVN_SETTINGS\"}");
                final String globalSettings =
                        TokenMacro.expandAll(build, listener, "${ENV, var=\"MVN_GLOBALSETTINGS\"}");
                assertTrue(StringUtils.isNotBlank(userSettings), "env variable for user settings is not set");
                assertTrue(StringUtils.isNotBlank(globalSettings), "env variable for global settings is not set");
            } catch (MacroEvaluationException e) {
                fail("not able to expand var: " + e.getMessage());
            }
            return true;
        }
    }

    private MavenSettingsConfig createSettings(JenkinsRule j, MavenSettingsConfigProvider provider) throws Exception {

        MavenSettingsConfig c1 = (MavenSettingsConfig) provider.newConfig();
        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    private GlobalMavenSettingsConfig createGlobalSettings(JenkinsRule j, GlobalMavenSettingsConfigProvider provider)
            throws Exception {

        GlobalMavenSettingsConfig c1 = (GlobalMavenSettingsConfig) provider.newConfig();
        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }
}
