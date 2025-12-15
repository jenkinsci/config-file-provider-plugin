package org.jenkinsci.plugins.configfiles.maven.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import hudson.Functions;
import hudson.maven.MavenModuleSet;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Maven;
import jakarta.inject.Inject;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTextInput;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 * @author Dominik Bartholdi (imod)
 */
@WithJenkins
class MvnSettingsProviderTest {

    @Inject
    MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    @Issue("JENKINS-15976")
    void testConfigRoundtrip(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(jenkins, mavenSettingProvider);
        Config c2 = createSetting(jenkins, globalMavenSettingsConfigProvider);

        FreeStyleProject p = jenkins.createFreeStyleProject();
        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);
        Maven m = new Maven("install", null, null, null, null, false, s1, s2);
        p.getBuildersList().add(m);
        jenkins.configRoundtrip((Item) p);
        m = p.getBuildersList().get(Maven.class);

        jenkins.assertEqualDataBoundBeans(m.getSettings(), s1);
        jenkins.assertEqualDataBoundBeans(m.getGlobalSettings(), s2);
        assertNotSame(m.getSettings(), s1);
        assertNotSame(m.getGlobalSettings(), s2);
    }

    private Config createSetting(JenkinsRule jenkins, ConfigProvider provider) {
        Config c1 = provider.newConfig();
        GlobalConfigFiles globalConfigFiles =
                jenkins.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    @Test
    @Issue("JENKINS-15976")
    void testConfigRoundtripMavenJob(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(jenkins, mavenSettingProvider);
        Config c2 = createSetting(jenkins, globalMavenSettingsConfigProvider);

        MavenModuleSet p = jenkins.createProject(MavenModuleSet.class);
        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);
        p.setSettings(s1);
        p.setGlobalSettings(s2);
        jenkins.configRoundtrip((Item) p);

        jenkins.assertEqualDataBoundBeans(p.getSettings(), s1);
        jenkins.assertEqualDataBoundBeans(p.getGlobalSettings(), s2);
        assertNotSame(p.getSettings(), s1);
        assertNotSame(p.getGlobalSettings(), s2);
        if (Functions.isWindows()) {
            // Wait before exiting the test so that files close before cleanup
            Thread.sleep(3011);
        }
    }

    @Test
    @Issue("JENKINS-20403")
    void configMustBeVisibleFromConfigPage(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final WebClient client = jenkins.createWebClient();
        {
            Config c1 = createSetting(jenkins, mavenSettingProvider);
            // http://localhost:8080/configfiles/show?id=org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1383601194902
            final HtmlPage page = client.goTo("configfiles/show?id=" + c1.id);

            final HtmlTextInput name = page.getElementByName("config.name");
            final HtmlTextInput comment = page.getElementByName("config.comment");
            assertEquals(c1.name, name.getValue(), "name NOK");
            assertEquals(c1.comment, comment.getValue(), "comment NOK");
        }
        {
            // GlobalMavenSettingsConfig
            Config c2 = createSetting(jenkins, globalMavenSettingsConfigProvider);
            // http://localhost:8080/configfiles/show?id=org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig23435346436324
            final HtmlPage page = client.goTo("configfiles/show?id=" + c2.id);

            final HtmlTextInput name = page.getElementByName("config.name");
            final HtmlTextInput comment = page.getElementByName("config.comment");
            assertEquals(c2.name, name.getValue(), "name NOK");
            assertEquals(c2.comment, comment.getValue(), "comment NOK");
        }
    }

    @Test
    @Issue("JENKINS-40737")
    void mavenSettingsMustBeFoundInFreestyleProject(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final FreeStyleProject p = jenkins.createFreeStyleProject();

        String mvnName = ToolInstallations.configureMaven35().getName();
        Config c1 = createSetting(jenkins, mavenSettingProvider);
        Config c2 = createSetting(jenkins, globalMavenSettingsConfigProvider);

        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);

        Maven m = new Maven("clean", mvnName, null, null, null, false, s1, s2);
        p.getBuildersList().add(m);
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));

        jenkins.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new Cause.UserIdCause()).get());
    }

    @Test
    @Issue("JENKINS-40737")
    void notFoundMavenSettingsMustCauseBuildToFail(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final FreeStyleProject p = jenkins.createFreeStyleProject();

        String mvnName = ToolInstallations.configureMaven35().getName();

        MvnSettingsProvider s1 = new MvnSettingsProvider("dummyId");
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider("dummyGlobalId");

        Maven m = new Maven("clean", mvnName, null, null, null, false, s1, s2);
        p.getBuildersList().add(m);
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));

        jenkins.assertBuildStatus(
                Result.FAILURE, p.scheduleBuild2(0, new Cause.UserIdCause()).get());
    }
}
