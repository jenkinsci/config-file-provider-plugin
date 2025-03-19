package org.jenkinsci.plugins.configfiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by domi on 18/01/17.
 */
@WithJenkins
class ConfigFileStoreTest {

    private static final String CONFIG_ID = "myid";

    @Test
    void testSaveGlobalConfigFiles(JenkinsRule j) {
        GlobalConfigFiles store =
                j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        assertTrue(store.getConfigs().isEmpty());

        CustomConfig config = new CustomConfig(CONFIG_ID, "name", "comment", "content");
        store.save(config);

        assertEquals(1, store.getConfigs().size());
        Config savedConfig = store.getConfigs().iterator().next();
        assertEquals("name", savedConfig.name);
        assertEquals("comment", savedConfig.comment);
        assertEquals("content", savedConfig.content);

        CustomConfig anotherConfig = new CustomConfig("anotherid", "name", "comment", "content");
        store.save(anotherConfig);

        assertEquals(2, store.getConfigs().size());

        // Update config. Check the correct config is updated with proper values
        config = new CustomConfig(CONFIG_ID, "new name", "new comment", "new content");
        store.save(config);

        assertEquals(2, store.getConfigs().size());
        savedConfig = store.getById(CONFIG_ID);
        assertEquals("new name", savedConfig.name);
        assertEquals("new comment", savedConfig.comment);
        assertEquals("new content", savedConfig.content);

        // Remove config. Check the correct one is removed
        store.remove(savedConfig.id);
        assertEquals(1, store.getConfigs().size());
        assertThat(store.getById(anotherConfig.id), Matchers.is(anotherConfig));
        assertNull(store.getById(savedConfig.id));

        store.remove(anotherConfig.id);
        assertEquals(0, store.getConfigs().size());
    }
}
