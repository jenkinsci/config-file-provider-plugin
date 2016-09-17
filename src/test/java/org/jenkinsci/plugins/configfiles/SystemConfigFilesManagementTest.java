package org.jenkinsci.plugins.configfiles;

import hudson.ExtensionList;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.Collection;

/**
 * Created by domi on 17/09/16.
 */
public class SystemConfigFilesManagementTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testLoadAndMergeOldData() {

//        ExtensionList<ConfigFilesManagement> mgmts = j.getInstance().getExtensionList(ConfigFilesManagement.class);
//        ConfigFilesManagement configFilesManagement = mgmts.get(0);
//        Assert.assertEquals(4, configFilesManagement.getConfigs().size());

        Assert.assertEquals(4, GlobalConfigFiles.get().getConfigs().size());

    }
}
