package org.jenkinsci.plugins.configfiles;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileAction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

/**
 * Created by domi on 18/01/17.
 */
public class ConfigFileStoreTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSaveGlobalConfigFiles() {

        GlobalConfigFiles store = j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        Assert.assertTrue(store.getConfigs().isEmpty());

        CustomConfig config = new CustomConfig("myid", "name", "comment", "content");
        store.save(config);

        Assert.assertEquals(1, store.getConfigs().size());
        Config savedConfig = store.getConfigs().iterator().next();
        Assert.assertEquals("name", savedConfig.name);
        Assert.assertEquals("comment", savedConfig.comment);
        Assert.assertEquals("content", savedConfig.content);

        config = new CustomConfig("myid", "new name", "new comment", "new content");
        store.save(config);

        savedConfig = store.getConfigs().iterator().next();
        Assert.assertEquals(1, store.getConfigs().size());
        Assert.assertEquals("new name", savedConfig.name);
        Assert.assertEquals("new comment", savedConfig.comment);
        Assert.assertEquals("new content", savedConfig.content);
    }

}
