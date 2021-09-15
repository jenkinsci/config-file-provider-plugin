package org.jenkinsci.plugins.configfiles;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class GlobalConfigFilesConfigAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("GlobalConfigFilesConfigAsCodeTest.yaml")
    public void should_set_config_files() {
        final GlobalConfigFiles cfg = GlobalConfigFiles.get();
        final Config custom = cfg.getById("custom-test");
        Assert.assertNotNull(custom);
        Assert.assertEquals("dummy content 1", custom.content);
        final Config json = cfg.getById("json-test");
        Assert.assertNotNull(json);
        Assert.assertEquals("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }", json.content);
        final Config properties = cfg.getById("properties-test");
        Assert.assertNotNull(properties);
        Assert.assertEquals("myPropertyKey=myPropertyVal", properties.content);
        final Config xml = cfg.getById("xml-test");
        Assert.assertNotNull(xml);
        Assert.assertEquals("<root><dummy test=\"abc\"></dummy></root>", xml.content);
        final MavenSettingsConfig maven = (MavenSettingsConfig) cfg.getById("maven-test");
        Assert.assertNotNull(maven);
        Assert.assertFalse(maven.isReplaceAll);
        Assert.assertEquals("someCredentials", maven.getServerCredentialMappings().get(0).getCredentialsId());
    }

    /** @see https://issues.jenkins.io/browse/JENKINS-60498 */
    @Test
    public void ensure_configs_treeset() {
        final Config[] testConfigs = {
            new CustomConfig("1", "name1", "", ""),
            new CustomConfig("2", "name2", "", "")
        };

        final GlobalConfigFiles cfg = GlobalConfigFiles.get();
        cfg.setConfigs(Arrays.asList(testConfigs));

        final Collection<Config> actualConfigs = cfg.getConfigs();
        Assert.assertTrue("configs must be a TreeSet", actualConfigs instanceof TreeSet);
        MatcherAssert.assertThat(actualConfigs, Matchers.containsInAnyOrder(testConfigs));
    }
}
