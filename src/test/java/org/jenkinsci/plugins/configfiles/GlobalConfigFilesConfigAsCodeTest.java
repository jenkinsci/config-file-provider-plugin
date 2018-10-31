package org.jenkinsci.plugins.configfiles;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.lib.configprovider.model.Config;
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
        final Config xml = cfg.getById("xml-test");
        Assert.assertNotNull(xml);
        Assert.assertEquals("<root><dummy test=\"abc\"></dummy></root>", xml.content);
        final MavenSettingsConfig maven = (MavenSettingsConfig) cfg.getById("maven-test");
        Assert.assertNotNull(maven);
        Assert.assertFalse(maven.isReplaceAll);
        Assert.assertEquals("someCredentials", maven.getServerCredentialMappings().get(0).getCredentialsId());
    }
}
