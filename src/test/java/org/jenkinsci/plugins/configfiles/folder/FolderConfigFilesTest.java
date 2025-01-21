package org.jenkinsci.plugins.configfiles.folder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.cloudbees.hudson.plugins.folder.Folder;
import java.util.Collection;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Test for {@link FolderConfigFileProperty} to ensure reading data.
 */
@WithJenkins
class FolderConfigFilesTest {

    /**
     * Read data produced by Jenkins ver. 2.141 and plugin version 2.18.<br>
     * The only change I made is to downgrade XML version to 1.0 because of this:<br>
     *
     * XML version of descriptor are upgraded from 1.0 to 1.1 in 2.105 https://jenkins.io/changelog/#v2.105 (JENKINS-48463)
     */
    @LocalData
    @Test
    void verifyLoadWithAnonymousInnerClassComparatorVar1(JenkinsRule j) {
        ConfigFileStore store = ((Folder) j.jenkins.getItemByFullName("test-folder"))
                .getAction(FolderConfigFileAction.class)
                .getStore();
        Collection<Config> configs = store.getConfigs();
        assertThat(configs, hasSize(2));
    }
}
