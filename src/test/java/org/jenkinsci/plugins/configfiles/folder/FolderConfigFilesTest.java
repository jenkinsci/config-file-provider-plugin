package org.jenkinsci.plugins.configfiles.folder;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Test for {@link FolderConfigFileProperty} to ensure reading data.
 */
public class FolderConfigFilesTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Read data produced by Jenkins ver. 2.141 and plugin version 2.18.<br>
     * The only change I made is to downgrade XML version to 1.0 because of this:<br>
     *
     * XML version of descriptor are upgraded from 1.0 to 1.1 in 2.105 https://jenkins.io/changelog/#v2.105 (JENKINS-48463)
     */
    @LocalData
    @Test
    public void verifyLoadWithAnonymousInnerClassComparatorVar1() {
        ConfigFileStore store = ((Folder) j.jenkins.getItemByFullName("test-folder")).getAction(FolderConfigFileAction.class).getStore();
        Collection<Config> configs = store.getConfigs();
        assertThat(configs, hasSize(2));
    }
}
