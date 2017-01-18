package org.jenkinsci.lib.configprovider;

import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Created by domi on 17/09/16.
 */
public class SystemConfigFilesManagementTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testLoadAndMergeOldData() {
        // testLoadAndMergeOldData.zip contains different configurations files (as used in the old storrage implementation)
        // after starting, jenkins, all these have to be imported into GlobalConfigFiles
        Assert.assertEquals(4, GlobalConfigFiles.get().getConfigs().size());

        for (ConfigProvider cp : ConfigProvider.all()) {
            // as all the config files have been moved to global config,
            // all providers must not hold any files any more
            AbstractConfigProviderImpl acp = (AbstractConfigProviderImpl) cp;
            Assert.assertTrue(acp.getConfigs().isEmpty());
            Assert.assertFalse(acp.getConfigXml().getFile().exists());
        }
    }
}
