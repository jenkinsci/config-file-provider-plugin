package org.jenkinsci.plugins.configfiles;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.jenkinsci.lib.configprovider.model.Config;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Test for GlobalConfigFiles to ensure reading data.<br>
 */
public class GlobalConfigFilesTest {

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
    public void verifyLoadWithAnonymousInnerClasseComperatorVar1() {
        GlobalConfigFiles store = j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        Collection<Config> configs = store.getConfigs();
        assertThat(configs, hasSize(2));
    }

}
