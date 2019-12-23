package org.jenkinsci.plugins.configfiles.folder;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by domi on 11/10/16.
 */
public class FolderConfigFileActionTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    @Rule
    public BuildWatcher buildWatcher = new BuildWatcher();

    @Test
    public void foldersHaveTheirOwnListOfConfigs() throws Exception {
        Folder f1 = createFolder();
        getStore(f1).save(newCustomFile());

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = getStore(f1).getGroupedConfigs();
        assertNotNull(groupedConfigs1);
        Folder f2 = createFolder();
        getStore(f2).save(newCustomFile());

        Map<ConfigProvider, Collection<Config>> groupedConfigs2 = getStore(f2).getGroupedConfigs();
        assertNotNull(groupedConfigs2);
        assertNotEquals(groupedConfigs1, groupedConfigs2);
    }

    @Test
    public void foldersConfigListIsOrderedByName() throws Exception {
        Folder f1 = createFolder();
        getStore(f1).save(newCustomFileWithName("aaaa1", "zzzz"));
        getStore(f1).save(newCustomFileWithName("bbbb1", "yyyy"));
        getStore(f1).save(newCustomFileWithName("zzzz1", "aaaa"));
        getStore(f1).save(newCustomFileWithName("yyyy1", "bbbb"));

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = getStore(f1).getGroupedConfigs();
        assertNotNull(groupedConfigs1);

        assertEquals(1, groupedConfigs1.entrySet().size());
        // casting to ease test case, List is not in the public API
        List<Config> configs = (List<Config>) groupedConfigs1.entrySet().iterator().next().getValue();
        assertEquals("aaaa", configs.get(0).name);
        assertEquals("bbbb", configs.get(1).name);
        assertEquals("yyyy", configs.get(2).name);
        assertEquals("zzzz", configs.get(3).name);
    }

    @Test
    public void globalConfigListIsOrderedByName() throws Exception {

        GlobalConfigFiles globalConfigFiles = r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);

        globalConfigFiles.save(newCustomFileWithName("aaaa1", "zzzz"));
        globalConfigFiles.save(newCustomFileWithName("bbbb1", "yyyy"));
        globalConfigFiles.save(newCustomFileWithName("zzzz1", "aaaa"));
        globalConfigFiles.save(newCustomFileWithName("yyyy1", "bbbb"));

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = globalConfigFiles.getGroupedConfigs();
        assertNotNull(groupedConfigs1);

        assertEquals(1, groupedConfigs1.entrySet().size());
        // casting to ease test case, List is not in the public API
        List<Config> configs = (List<Config>) groupedConfigs1.entrySet().iterator().next().getValue();
        assertEquals("aaaa", configs.get(0).name);
        assertEquals("bbbb", configs.get(1).name);
        assertEquals("yyyy", configs.get(2).name);
        assertEquals("zzzz", configs.get(3).name);
    }

    @Test
    public void configsInContextAreOrderedByName() throws Exception {

        GlobalConfigFiles globalConfigFiles = r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);

        globalConfigFiles.save(newCustomFileWithName("aaaa1", "zzzz"));
        globalConfigFiles.save(newCustomFileWithName("bbbb1", "yyyy"));
        globalConfigFiles.save(newCustomFileWithName("zzzz1", "aaaa"));
        globalConfigFiles.save(newCustomFileWithName("yyyy1", "bbbb"));

        List<Config> configsInContext = ConfigFiles.getConfigsInContext(r.jenkins, CustomConfig.CustomConfigProvider.class);

        assertEquals("aaaa", configsInContext.get(0).name);
        assertEquals("bbbb", configsInContext.get(1).name);
        assertEquals("yyyy", configsInContext.get(2).name);
        assertEquals("zzzz", configsInContext.get(3).name);
    }

    /*
     * see details to this test: https://gist.github.com/cyrille-leclerc/5571fdc443d6f7bff4e5ec10d614a15d
     */
    @Test
    public void accessGlobalFilesFromWithinFolder() throws Exception {
        GlobalConfigFiles globalConfigFiles = r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);
        Collection<Config> configs = globalConfigFiles.getConfigs();
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
        globalConfigFiles.save(newMvnSettings("my-file-id"));

        WorkflowJob jobInRoot = r.jenkins.createProject(WorkflowJob.class, "p");
        jobInRoot.setDefinition(getNewJobDefinition());

        Folder folder1 = createFolder();
        getStore(folder1).save(newMvnSettings("my-file-id"));

        WorkflowJob jobInFolder1 = folder1.createProject(WorkflowJob.class, "p");
        jobInFolder1.setDefinition(getNewJobDefinition());

        Folder folder2 = createFolder();


        WorkflowJob jobInFolder2 = folder2.createProject(WorkflowJob.class, "p");
        jobInFolder2.setDefinition(getNewJobDefinition());

        WorkflowRun b0 = r.assertBuildStatusSuccess(jobInRoot.scheduleBuild2(0));
        WorkflowRun b1 = r.assertBuildStatusSuccess(jobInFolder1.scheduleBuild2(0));
        WorkflowRun b2 = r.assertBuildStatusSuccess(jobInFolder2.scheduleBuild2(0));
    }

    @Test
    public void correctFileMustBeSelectedInHierarchy() throws Exception {
        GlobalConfigFiles globalConfigFiles = r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);
        Collection<Config> configs = globalConfigFiles.getConfigs();
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
        globalConfigFiles.save(newCustomFile("my-file-id", "Hello Root"));

        WorkflowJob jobInRoot = r.jenkins.createProject(WorkflowJob.class, "p");
        jobInRoot.setDefinition(getNewJobDefinition());

        Folder folder1 = createFolder();
        getStore(folder1).save(newCustomFile("my-file-id", "Hello Folder1"));

        WorkflowJob jobInFolder1 = folder1.createProject(WorkflowJob.class, "p");
        jobInFolder1.setDefinition(getNewJobDefinition());

        WorkflowRun b0 = r.assertBuildStatusSuccess(jobInRoot.scheduleBuild2(0));
        r.assertLogContains("Hello Root", b0);
        WorkflowRun b1 = r.assertBuildStatusSuccess(jobInFolder1.scheduleBuild2(0));
        r.assertLogContains("Hello Folder1", b1);
    }

    @Test
    public void testSaveFolderConfigFiles() throws Exception {

        Folder f1 = createFolder();
        ConfigFileStore store = getStore(f1);

        CustomConfig config = new CustomConfig("myid", "name", "comment", "content");
        store.save(config);

        assertEquals(1, store.getConfigs().size());
        Config savedConfig = store.getConfigs().iterator().next();
        assertEquals("name", savedConfig.name);
        assertEquals("comment", savedConfig.comment);
        assertEquals("content", savedConfig.content);

        config = new CustomConfig("myid", "new name", "new comment", "new content");
        store.save(config);

        savedConfig = store.getConfigs().iterator().next();
        assertEquals(1, store.getConfigs().size());
        assertEquals("new name", savedConfig.name);
        assertEquals("new comment", savedConfig.comment);
        assertEquals("new content", savedConfig.content);

    }

    @Test
    public void sameFolderPropertyAfterConfiguration() throws Exception {
        Folder f1 = createFolder();
        ConfigFileStore store = getStore(f1);

        r.configRoundtrip(f1);

        assertThat(store, Matchers.is(getStore(f1)));
    }

    private CpsFlowDefinition getNewJobDefinition() {
        return new CpsFlowDefinition("" +
                "node {\n" +
                "  configFileProvider([configFile(fileId: 'my-file-id', variable: 'MY_FILE')]) {\n" +
                "    if (isUnix()) {\n" +
                "      sh '''\n" +
                "      ls -al \"$MY_FILE\"\n" +
                "      cat \"$MY_FILE\"\n" +
                "      '''\n" +
                "    } else {\n" +
                "      bat '''\n" +
                "      dir /a \"%MY_FILE%\"\n" +
                "      type \"%MY_FILE%\"\n" +
                "      '''\n" +
                "    }\n" +
                "  }\n" +
                "}", true);
    }


    private ConfigFileStore getStore(Folder f) {
        FolderConfigFileAction action = f.getAction(FolderConfigFileAction.class);
        return action.getStore();
    }

    private Folder createFolder() throws IOException {
        return r.jenkins.createProject(Folder.class, "folder" + r.jenkins.getItems().size());
    }

    private Config newCustomFile() {
        CustomConfig.CustomConfigProvider configProvider = r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return configProvider.newConfig("custom_" + System.currentTimeMillis());
    }

    private Config newCustomFile(String id, String content) {
        CustomConfig.CustomConfigProvider configProvider = r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return new CustomConfig(id, "custom.txt", "custom file", content, configProvider.getProviderId());
    }

    private Config newCustomFileWithName(String id, String name) {
        CustomConfig.CustomConfigProvider configProvider = r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return new CustomConfig(id, name, "custom file", "dummy content", configProvider.getProviderId());
    }

    private Config newMvnSettings(String settingsId) {
        MavenSettingsConfig.MavenSettingsConfigProvider configProvider = r.jenkins.getExtensionList(ConfigProvider.class).get(MavenSettingsConfig.MavenSettingsConfigProvider.class);
        return configProvider.newConfig(settingsId);
    }
}
