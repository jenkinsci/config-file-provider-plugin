package org.jenkinsci.lib.configprovider;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.groovy.GroovyScript;
import org.jenkinsci.plugins.configfiles.json.JsonConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by domi on 17/09/16.
 */
public class SystemConfigFilesManagementTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testLoadAndMergeOldData() {

        for (ConfigProvider cp : ConfigProvider.all()) {
            // as all the config files have been moved to global config,
            // all providers must not hold any files any more
            AbstractConfigProviderImpl acp = (AbstractConfigProviderImpl) cp;
            Assert.assertTrue("configs for " + acp.getProviderId() + " should be empty", acp.getConfigs().isEmpty());
            Assert.assertFalse("file for " + acp.getProviderId() + " still exists", acp.getConfigXml().getFile().exists());
        }

        Assert.assertEquals(1, getProvider(MavenSettingsConfig.MavenSettingsConfigProvider.class).getAllConfigs().size());
        Assert.assertEquals(1, getProvider(JsonConfig.JsonConfigProvider.class).getAllConfigs().size());
        Assert.assertEquals(1, getProvider(XmlConfig.XmlConfigProvider.class).getAllConfigs().size());
        Assert.assertEquals(1, getProvider(GroovyScript.GroovyConfigProvider.class).getAllConfigs().size());
        Assert.assertEquals(1, getProvider(CustomConfig.CustomConfigProvider.class).getAllConfigs().size());
//        Assert.assertEquals(1, getProvider(JellyTemplateConfig.JellyTemplateConfigProvider.class).getAllConfigs().size());

        Assert.assertEquals(5, GlobalConfigFiles.get().getConfigs().size());
    }

    private <T> T getProvider(Class<T> providerClass) {
        return j.getInstance().getExtensionList(providerClass).get(providerClass);
    }


    @Test
    public void testDynamicCreationOfConfigs() {
        for (ConfigProvider cp : ConfigProvider.all()) {
            Config config = cp.newConfig("myid", "myname", "mycomment", "mycontent");
            Assert.assertNotNull(config);
            Assert.assertEquals(config.id, "myid");
            Assert.assertEquals(config.name, "myname");
            Assert.assertEquals(config.comment, "mycomment");
            Assert.assertEquals(config.content, "mycontent");
            Assert.assertNotNull(config.getProviderId());
        }
    }

    @Test
    public void testDynamicCreationOfConfigs2() {
        final String id = "ExtensionPointTestConfigProvider-file-id";

        ExtensionPointTestConfig.ExtensionPointTestConfigProvider configProvider = getProvider(ExtensionPointTestConfig.ExtensionPointTestConfigProvider.class);
        Config newConfig = configProvider.newConfig(id);
        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(newConfig);

        Assert.assertEquals(1, GlobalConfigFiles.get().getConfigs(ExtensionPointTestConfig.ExtensionPointTestConfigProvider.class).size());

        ExtensionPointTestConfig savedConfig = (ExtensionPointTestConfig) GlobalConfigFiles.get().getConfigs(ExtensionPointTestConfig.ExtensionPointTestConfigProvider.class).iterator().next();
        Assert.assertEquals(savedConfig.id, id);
        Assert.assertEquals(savedConfig.name, ExtensionPointTestConfig.TEST_NAME_VALUE);
        Assert.assertEquals(savedConfig.comment, ExtensionPointTestConfig.TEST_COMMENT_VALUE);
        Assert.assertEquals(savedConfig.content, ExtensionPointTestConfig.TEST_CONTENT_VALUE);
        Assert.assertEquals(savedConfig.newParam1, ExtensionPointTestConfig.TEST_PARAM_VALUE);
        Assert.assertEquals(savedConfig.newParam2, ExtensionPointTestConfig.TEST_PARAM_VALUE);
        Assert.assertEquals(savedConfig.newParam3, ExtensionPointTestConfig.TEST_PARAM_VALUE);
        Assert.assertNotNull(savedConfig.getProviderId());
    }

    public static class ExtensionPointTestConfig extends Config {
        public static final String TEST_PARAM_VALUE = "DEFAULT_VALUE";
        public static final String TEST_NAME_VALUE = "ExtensionPointTestConfig";
        public static final String TEST_COMMENT_VALUE = "Test comment";
        public static final String TEST_CONTENT_VALUE = "Test content";

        private static final long serialVersionUID = 1L;

        public String newParam1;
        public String newParam2;
        public String newParam3;

        @DataBoundConstructor
        public ExtensionPointTestConfig(String id, String name, String comment, String content) {
            super(id, name, comment, content);
            newParam1 = TEST_PARAM_VALUE;
            newParam2 = TEST_PARAM_VALUE;
            newParam3 = TEST_PARAM_VALUE;
        }

        public ExtensionPointTestConfig(String id, String name, String comment, String content, String providerId) {
            super(id, name, comment, content, providerId);
            newParam1 = TEST_PARAM_VALUE;
            newParam2 = TEST_PARAM_VALUE;
            newParam3 = TEST_PARAM_VALUE;
        }

        public ExtensionPointTestConfig(String id, String name, String comment, String content, String providerId, String newParam1, String newParam2, String newParam3) {
            super(id, name, comment, content, providerId);
            this.newParam1 = newParam1;
            this.newParam2 = newParam2;
            this.newParam3 = newParam3;
        }

        @TestExtension("testDynamicCreationOfConfigs2")
        public static class ExtensionPointTestConfigProvider extends AbstractConfigProviderImpl {

            public ExtensionPointTestConfigProvider() {
                load();
            }

            @Override
            public ContentType getContentType() {
                return ContentType.DefinedType.GROOVY;
            }

            @Override
            public String getDisplayName() {
                return Messages.groovy_provider_name();
            }

            @NonNull
            @Override
            public Config newConfig(@NonNull String id) {
                return new ExtensionPointTestConfig(id, TEST_NAME_VALUE, TEST_COMMENT_VALUE, TEST_CONTENT_VALUE, getProviderId(), TEST_PARAM_VALUE, TEST_PARAM_VALUE, TEST_PARAM_VALUE);
            }

            // ======================
            // stuff for backward compatibility
            protected transient String ID_PREFIX;

            @Override
            protected String getXmlFileName() {
                return "extension-point-test-config-files.xml";
            }

            static {
                Jenkins.XSTREAM.alias("org.jenkinsci.lib.configprovider.ExtensionPointTestConfigProvider", ExtensionPointTestConfigProvider.class);
            }
            // ======================
        }

    }
}
