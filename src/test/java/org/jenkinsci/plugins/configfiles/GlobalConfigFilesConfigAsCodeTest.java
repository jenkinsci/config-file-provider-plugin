package org.jenkinsci.plugins.configfiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class GlobalConfigFilesConfigAsCodeTest {

    @Test
    @ConfiguredWithCode("GlobalConfigFilesConfigAsCodeTest.yaml")
    void should_set_config_files(JenkinsConfiguredWithCodeRule j) {
        final GlobalConfigFiles cfg = GlobalConfigFiles.get();
        final Config custom = cfg.getById("custom-test");
        assertNotNull(custom);
        assertEquals("dummy content 1", custom.content);
        final Config json = cfg.getById("json-test");
        assertNotNull(json);
        assertEquals("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }", json.content);
        final Config properties = cfg.getById("properties-test");
        assertNotNull(properties);
        assertEquals("myPropertyKey=myPropertyVal", properties.content);
        final Config xml = cfg.getById("xml-test");
        assertNotNull(xml);
        assertEquals("<root><dummy test=\"abc\"></dummy></root>", xml.content);
        final MavenSettingsConfig maven = (MavenSettingsConfig) cfg.getById("maven-test");
        assertNotNull(maven);
        assertFalse(maven.isReplaceAll);
        assertEquals(
                "someCredentials", maven.getServerCredentialMappings().get(0).getCredentialsId());
    }

    /** @see https://issues.jenkins.io/browse/JENKINS-60498 */
    @Test
    void ensure_configs_treeset(JenkinsConfiguredWithCodeRule j) {
        final Config[] testConfigs = {new CustomConfig("1", "name1", "", ""), new CustomConfig("2", "name2", "", "")};

        final GlobalConfigFiles cfg = GlobalConfigFiles.get();
        cfg.setConfigs(Arrays.asList(testConfigs));

        final Collection<Config> actualConfigs = cfg.getConfigs();
        assertInstanceOf(TreeSet.class, actualConfigs, "configs must be a TreeSet");
        assertThat(actualConfigs, Matchers.containsInAnyOrder(testConfigs));
    }
}
