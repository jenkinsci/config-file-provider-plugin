package org.jenkinsci.plugins.configfiles;

import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by domi on 18/01/17.
 */
public class ConfigFileStoreTest {

    private static final String CONFIG_ID = "myid";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSaveGlobalConfigFiles() {
        GlobalConfigFiles store = j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        Assert.assertTrue(store.getConfigs().isEmpty());

        CustomConfig config = new CustomConfig(CONFIG_ID, "name", "comment", "content");
        store.save(config);

        Assert.assertEquals(1, store.getConfigs().size());
        Config savedConfig = store.getConfigs().iterator().next();
        Assert.assertEquals("name", savedConfig.name);
        Assert.assertEquals("comment", savedConfig.comment);
        Assert.assertEquals("content", savedConfig.content);

        CustomConfig anotherConfig = new CustomConfig("anotherid", "name", "comment", "content");
        store.save(anotherConfig);

        Assert.assertEquals(2, store.getConfigs().size());

        // Update config. Check the correct config is updated with proper values
        config = new CustomConfig(CONFIG_ID, "new name", "new comment", "new content");
        store.save(config);

        Assert.assertEquals(2, store.getConfigs().size());
        savedConfig = store.getById(CONFIG_ID);
        Assert.assertEquals("new name", savedConfig.name);
        Assert.assertEquals("new comment", savedConfig.comment);
        Assert.assertEquals("new content", savedConfig.content);

        // Remove config. Check the correct one is removed
        store.remove(savedConfig.id);
        Assert.assertEquals(1, store.getConfigs().size());
        Assert.assertThat(store.getById(anotherConfig.id), Matchers.<Config>is(anotherConfig));
        Assert.assertNull(store.getById(savedConfig.id));

        store.remove(anotherConfig.id);
        Assert.assertEquals(0, store.getConfigs().size());
    }

}
