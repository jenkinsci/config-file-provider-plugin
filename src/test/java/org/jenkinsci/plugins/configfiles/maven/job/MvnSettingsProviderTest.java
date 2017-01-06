package org.jenkinsci.plugins.configfiles.maven.job;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.maven.MavenModuleSet;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Maven;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.ToolInstallations;

import javax.inject.Inject;

import static org.junit.Assert.assertNotSame;

/**
 * @author Kohsuke Kawaguchi
 * @author Dominik Bartholdi (imod)
 */
public class MvnSettingsProviderTest {
    @Rule
    public JenkinsRule                jenkins = new JenkinsRule();

    @Inject
    ConfigFilesManagement             config;

    @Inject
    MavenSettingsConfigProvider       mavenSettingProvider;

    @Inject
    GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test
    @Bug(15976)
    public void testConfigRoundtrip() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(mavenSettingProvider);
        Config c2 = createSetting(globalMavenSettingsConfigProvider);

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

    private Config createSetting(ConfigProvider provider) {
        Config c1 = provider.newConfig();
        GlobalConfigFiles globalConfigFiles = jenkins.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    @Test
    @Bug(15976)
    public void testConfigRoundtripMavenJob() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(mavenSettingProvider);
        Config c2 = createSetting(globalMavenSettingsConfigProvider);

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
    }

    @Test
    @Bug(20403)
    public void configMustBeVisbleFromConfigPage() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final WebClient client = jenkins.createWebClient();
        {
            Config c1 = createSetting(mavenSettingProvider);
            // http://localhost:8080/configfiles/show?id=org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1383601194902
            final HtmlPage page = client.goTo("configfiles/show?id=" + c1.id);

            final HtmlTextInput name = page.getElementByName("config.name");
            final HtmlTextInput comment = page.getElementByName("config.comment");
            Assert.assertEquals("name NOK", c1.name, name.getValueAttribute());
            Assert.assertEquals("comment NOK", c1.comment, comment.getValueAttribute());
        }
        {
            // GlobalMavenSettingsConfig
            Config c2 = createSetting(globalMavenSettingsConfigProvider);
            // http://localhost:8080/configfiles/show?id=org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig23435346436324
            final HtmlPage page = client.goTo("configfiles/show?id=" + c2.id);

            final HtmlTextInput name = page.getElementByName("config.name");
            final HtmlTextInput comment = page.getElementByName("config.comment");
            Assert.assertEquals("name NOK", c2.name, name.getValueAttribute());
            Assert.assertEquals("comment NOK", c2.comment, comment.getValueAttribute());
        }
    }

    @Test
    @Bug(40737)
    public void mavenSettingsMustBeFoundInFreestyleProject() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final FreeStyleProject p = jenkins.createFreeStyleProject();

        String mvnName = ToolInstallations.configureMaven3().getName();
        Config c1 = createSetting(mavenSettingProvider);
        Config c2 = createSetting(globalMavenSettingsConfigProvider);

        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);

        Maven m = new Maven("clean", mvnName, null, null, null, false, s1, s2);
        p.getBuildersList().add(m);
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));

        jenkins.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new Cause.UserCause()).get());
    }

    @Test
    @Bug(40737)
    public void notFoundMavenSettingsMustCauseBuildToFail() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        final FreeStyleProject p = jenkins.createFreeStyleProject();

        String mvnName = ToolInstallations.configureMaven3().getName();

        MvnSettingsProvider s1 = new MvnSettingsProvider("dummyId");
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider("dummyGlobalId");

        Maven m = new Maven("clean", mvnName, null, null, null, false, s1, s2);
        p.getBuildersList().add(m);
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));

        jenkins.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0, new Cause.UserCause()).get());
    }
}
