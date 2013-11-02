package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.maven.MavenModuleSet;
import hudson.model.Item;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;

import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

import javax.inject.Inject;

import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class MvnSettingsProviderTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Inject
    ConfigFilesManagement config;

    @Inject
    MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    @Test @Bug(15976)
    public void testConfigRoundtrip() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(mavenSettingProvider);
        Config c2 = createSetting(globalMavenSettingsConfigProvider);

        FreeStyleProject p = jenkins.createFreeStyleProject();
        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);
        Maven m = new Maven("install", null,null,null,null,false, s1, s2);
        p.getBuildersList().add(m);
        jenkins.configRoundtrip((Item)p);
        m = p.getBuildersList().get(Maven.class);

        jenkins.assertEqualDataBoundBeans(m.getSettings(),s1);
        jenkins.assertEqualDataBoundBeans(m.getGlobalSettings(),s2);
        assertNotSame(m.getSettings(), s1);
        assertNotSame(m.getGlobalSettings(),s2);
    }

    private Config createSetting(ConfigProvider provider) {
        Config c1 = provider.newConfig();
        provider.save(c1);
        return c1;
    }

    @Test @Bug(15976)
    public void testConfigRoundtripMavenJob() throws Exception {
        jenkins.jenkins.getInjector().injectMembers(this);

        Config c1 = createSetting(mavenSettingProvider);
        Config c2 = createSetting(globalMavenSettingsConfigProvider);

        MavenModuleSet p = jenkins.createMavenProject();
        MvnSettingsProvider s1 = new MvnSettingsProvider(c1.id);
        MvnGlobalSettingsProvider s2 = new MvnGlobalSettingsProvider(c2.id);
        p.setSettings(s1);
        p.setGlobalSettings(s2);
        jenkins.configRoundtrip((Item)p);

        jenkins.assertEqualDataBoundBeans(p.getSettings(),s1);
        jenkins.assertEqualDataBoundBeans(p.getGlobalSettings(),s2);
        assertNotSame(p.getSettings(), s1);
        assertNotSame(p.getGlobalSettings(),s2);
    }
}
