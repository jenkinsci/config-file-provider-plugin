package org.jenkinsci.plugins.configfiles;

import static org.junit.jupiter.api.Assertions.*;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Item;
import hudson.model.ItemGroup;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.springframework.security.access.AccessDeniedException;

@WithJenkins
class ConfigFilesTest {

    private static final String CONFIG_ID = "ConfigFilesTestId";

    @Test
    void testConfigContextResolver(JenkinsRule j) {
        GlobalConfigFiles store =
                j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        assertTrue(store.getConfigs().isEmpty());

        CustomConfig config = new CustomConfig(CONFIG_ID, "name", "comment", "content");
        store.save(config);

        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigFiles.getByIdOrNull(new TestItemGroup(), CONFIG_ID),
                "should have thrown");

        j.getInstance().getExtensionList(ConfigContextResolver.class).get(TestResolver.class).isActive = true;

        Config retrievedConfig = ConfigFiles.getByIdOrNull(new TestItemGroup(), CONFIG_ID);
        assertNotNull(retrievedConfig);
    }

    @TestExtension
    public static class TestResolver extends ConfigContextResolver {
        private boolean isActive = false;

        @Override
        public ItemGroup getConfigContext(ItemGroup itemGroup) {
            if (isActive) {
                return Jenkins.get();
            }
            return null;
        }
    }

    private class TestItemGroup implements ItemGroup<Item> {
        @Override
        public String getFullName() {
            return null;
        }

        @Override
        public String getFullDisplayName() {
            return null;
        }

        @Override
        public Collection<Item> getItems() {
            return null;
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUrlChildPrefix() {
            return null;
        }

        @CheckForNull
        @Override
        public Item getItem(String name) throws AccessDeniedException {
            return null;
        }

        @Override
        public File getRootDirFor(Item child) {
            return null;
        }

        @Override
        public void onRenamed(Item item, String oldName, String newName) throws IOException {}

        @Override
        public void onDeleted(Item item) throws IOException {}

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public File getRootDir() {
            return null;
        }

        @Override
        public void save() throws IOException {}
    }
}
