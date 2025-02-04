package org.jenkinsci.plugins.configfiles.folder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jenkins.model.Jenkins;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.HtmlElementUtil;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by domi on 11/10/16.
 */
@WithJenkins
class FolderConfigFileActionTest {

    @Test
    void foldersHaveTheirOwnListOfConfigs(JenkinsRule r) throws Exception {
        Folder f1 = createFolder(r);
        getStore(f1).save(newCustomFile(r));

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = getStore(f1).getGroupedConfigs();
        assertNotNull(groupedConfigs1);
        Folder f2 = createFolder(r);
        getStore(f2).save(newCustomFile(r));

        Map<ConfigProvider, Collection<Config>> groupedConfigs2 = getStore(f2).getGroupedConfigs();
        assertNotNull(groupedConfigs2);
        assertNotEquals(groupedConfigs1, groupedConfigs2);
    }

    @Test
    void foldersConfigListIsOrderedByName(JenkinsRule r) throws Exception {
        Folder f1 = createFolder(r);
        getStore(f1).save(newCustomFileWithName(r, "aaaa1", "zzzz"));
        getStore(f1).save(newCustomFileWithName(r, "bbbb1", "yyyy"));
        getStore(f1).save(newCustomFileWithName(r, "zzzz1", "aaaa"));
        getStore(f1).save(newCustomFileWithName(r, "yyyy1", "bbbb"));

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = getStore(f1).getGroupedConfigs();
        assertNotNull(groupedConfigs1);

        assertEquals(1, groupedConfigs1.entrySet().size());
        // casting to ease test case, List is not in the public API
        List<Config> configs =
                (List<Config>) groupedConfigs1.entrySet().iterator().next().getValue();
        assertEquals("aaaa", configs.get(0).name);
        assertEquals("bbbb", configs.get(1).name);
        assertEquals("yyyy", configs.get(2).name);
        assertEquals("zzzz", configs.get(3).name);
    }

    @Test
    void globalConfigListIsOrderedByName(JenkinsRule r) throws Exception {

        GlobalConfigFiles globalConfigFiles =
                r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);

        globalConfigFiles.save(newCustomFileWithName(r, "aaaa1", "zzzz"));
        globalConfigFiles.save(newCustomFileWithName(r, "bbbb1", "yyyy"));
        globalConfigFiles.save(newCustomFileWithName(r, "zzzz1", "aaaa"));
        globalConfigFiles.save(newCustomFileWithName(r, "yyyy1", "bbbb"));

        Map<ConfigProvider, Collection<Config>> groupedConfigs1 = globalConfigFiles.getGroupedConfigs();
        assertNotNull(groupedConfigs1);

        assertEquals(1, groupedConfigs1.entrySet().size());
        // casting to ease test case, List is not in the public API
        List<Config> configs =
                (List<Config>) groupedConfigs1.entrySet().iterator().next().getValue();
        assertEquals("aaaa", configs.get(0).name);
        assertEquals("bbbb", configs.get(1).name);
        assertEquals("yyyy", configs.get(2).name);
        assertEquals("zzzz", configs.get(3).name);
    }

    @Test
    void configsInContextAreOrderedByName(JenkinsRule r) throws Exception {

        GlobalConfigFiles globalConfigFiles =
                r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);

        globalConfigFiles.save(newCustomFileWithName(r, "aaaa1", "zzzz"));
        globalConfigFiles.save(newCustomFileWithName(r, "bbbb1", "yyyy"));
        globalConfigFiles.save(newCustomFileWithName(r, "zzzz1", "aaaa"));
        globalConfigFiles.save(newCustomFileWithName(r, "yyyy1", "bbbb"));

        List<Config> configsInContext =
                ConfigFiles.getConfigsInContext(r.jenkins, CustomConfig.CustomConfigProvider.class);

        assertEquals("aaaa", configsInContext.get(0).name);
        assertEquals("bbbb", configsInContext.get(1).name);
        assertEquals("yyyy", configsInContext.get(2).name);
        assertEquals("zzzz", configsInContext.get(3).name);
    }

    /*
     * see details to this test: https://gist.github.com/cyrille-leclerc/5571fdc443d6f7bff4e5ec10d614a15d
     */
    @Test
    void accessGlobalFilesFromWithinFolder(JenkinsRule r) throws Exception {
        GlobalConfigFiles globalConfigFiles =
                r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);
        Collection<Config> configs = globalConfigFiles.getConfigs();
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
        globalConfigFiles.save(newMvnSettings(r, "my-file-id"));

        WorkflowJob jobInRoot = r.jenkins.createProject(WorkflowJob.class, "p");
        jobInRoot.setDefinition(getNewJobDefinition());

        Folder folder1 = createFolder(r);
        getStore(folder1).save(newMvnSettings(r, "my-file-id"));

        WorkflowJob jobInFolder1 = folder1.createProject(WorkflowJob.class, "p");
        jobInFolder1.setDefinition(getNewJobDefinition());

        Folder folder2 = createFolder(r);

        WorkflowJob jobInFolder2 = folder2.createProject(WorkflowJob.class, "p");
        jobInFolder2.setDefinition(getNewJobDefinition());

        r.assertBuildStatusSuccess(jobInRoot.scheduleBuild2(0));
        r.assertBuildStatusSuccess(jobInFolder1.scheduleBuild2(0));
        r.assertBuildStatusSuccess(jobInFolder2.scheduleBuild2(0));
    }

    @Test
    void correctFileMustBeSelectedInHierarchy(JenkinsRule r) throws Exception {
        GlobalConfigFiles globalConfigFiles =
                r.jenkins.getExtensionList(ConfigFileStore.class).get(GlobalConfigFiles.class);
        Collection<Config> configs = globalConfigFiles.getConfigs();
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
        globalConfigFiles.save(newCustomFile(r, "my-file-id", "Hello Root"));

        WorkflowJob jobInRoot = r.jenkins.createProject(WorkflowJob.class, "p");
        jobInRoot.setDefinition(getNewJobDefinition());

        Folder folder1 = createFolder(r);
        getStore(folder1).save(newCustomFile(r, "my-file-id", "Hello Folder1"));

        WorkflowJob jobInFolder1 = folder1.createProject(WorkflowJob.class, "p");
        jobInFolder1.setDefinition(getNewJobDefinition());

        WorkflowRun b0 = r.assertBuildStatusSuccess(jobInRoot.scheduleBuild2(0));
        r.assertLogContains("Hello Root", b0);
        WorkflowRun b1 = r.assertBuildStatusSuccess(jobInFolder1.scheduleBuild2(0));
        r.assertLogContains("Hello Folder1", b1);
    }

    @Test
    void testSaveFolderConfigFiles(JenkinsRule r) throws Exception {

        Folder f1 = createFolder(r);
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
    void sameFolderPropertyAfterConfiguration(JenkinsRule r) throws Exception {
        Folder f1 = createFolder(r);
        ConfigFileStore store = getStore(f1);

        r.configRoundtrip(f1);

        assertThat(store, Matchers.is(getStore(f1)));
    }

    @Test
    @Issue("SECURITY-2202")
    void xssPreventionInFolder(JenkinsRule r) throws Exception {
        final String CONFIG_ID = "myid";

        // ----------
        // Create a new configuration in a new folder
        Folder f1 = createFolder(r);
        ConfigFileStore store = getStore(f1);

        CustomConfig config = new CustomConfig(CONFIG_ID, "name", "comment", "content");
        store.save(config);

        assertEquals(1, store.getConfigs().size());

        /// ----------
        // Check removing it by GET doesn't work
        JenkinsRule.WebClient wc = r.createWebClient();

        // If we try to call the URL directly (via GET), it fails with a 405 - Method not allowed
        wc.assertFails(f1.getUrl() + "configfiles/removeConfig?id=" + CONFIG_ID, 405);
        assertEquals(1, store.getConfigs().size());

        // ----------
        // Clicking the button works
        // If we click on the link, it goes via POST, therefore it removes it successfully
        HtmlPage configFiles = wc.goTo(f1.getUrl() + "configfiles");
        HtmlAnchor removeAnchor = configFiles
                .getDocumentElement()
                .getFirstByXPath("//a[contains(@data-url, 'removeConfig?id=" + CONFIG_ID + "')]");

        if (r.jenkins.getVersion().isOlderThan(new VersionNumber("2.415"))) {
            AtomicReference<Boolean> confirmCalled = new AtomicReference<>(false);
            wc.setConfirmHandler((page, s) -> {
                confirmCalled.set(true);
                return true;
            });
            assertThat(confirmCalled.get(), CoreMatchers.is(false));
            removeAnchor.click();
            assertThat(confirmCalled.get(), CoreMatchers.is(true));
        } else {
            HtmlElement document = configFiles.getDocumentElement();
            HtmlElementUtil.clickDialogOkButton(removeAnchor, document);
        }

        assertThat(store.getConfigs(), empty());
    }

    @Test
    @Issue("SECURITY-2203")
    void folderCheckConfigIdProtected(JenkinsRule r) throws Exception {
        // ----------
        // Create a new folder
        Folder f1 = createFolder(r);
        f1.save();

        // ----------
        // let's allow all people to see the folder, but not configure it
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                // read access to everyone
                .grant(Jenkins.READ)
                .everywhere()
                .toEveryone()
                .grant(Item.DISCOVER)
                .everywhere()
                .toAuthenticated()
                .grant(Item.READ)
                .onItems(f1)
                .toEveryone()

                // config access on the folder to this user
                .grant(Item.CONFIGURE)
                .onFolders(f1)
                .to("folderConfigurer"));

        // ----------
        // An user without permission cannot see the form to add a new config file
        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("reader");
        wc.assertFails(f1.getUrl() + "configfiles/selectProvider", 404);

        // ----------
        // The person with permission can access
        wc.login("folderConfigurer");
        HtmlPage page = wc.goTo(f1.getUrl() + "configfiles/selectProvider");
        MatcherAssert.assertThat(page, notNullValue());
    }

    private CpsFlowDefinition getNewJobDefinition() throws Descriptor.FormException {
        return new CpsFlowDefinition(
                "" + "node {\n"
                        + "  configFileProvider([configFile(fileId: 'my-file-id', variable: 'MY_FILE')]) {\n"
                        + "    if (isUnix()) {\n"
                        + "      sh '''\n"
                        + "      ls -al \"$MY_FILE\"\n"
                        + "      cat \"$MY_FILE\"\n"
                        + "      '''\n"
                        + "    } else {\n"
                        + "      bat '''\n"
                        + "      dir /a \"%MY_FILE%\"\n"
                        + "      type \"%MY_FILE%\"\n"
                        + "      '''\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                true);
    }

    private ConfigFileStore getStore(Folder f) {
        FolderConfigFileAction action = f.getAction(FolderConfigFileAction.class);
        return action.getStore();
    }

    private Folder createFolder(JenkinsRule r) throws IOException {
        return r.jenkins.createProject(
                Folder.class, "folder" + r.jenkins.getItems().size());
    }

    private Config newCustomFile(JenkinsRule r) {
        CustomConfig.CustomConfigProvider configProvider =
                r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return configProvider.newConfig("custom_" + System.currentTimeMillis());
    }

    private Config newCustomFile(JenkinsRule r, String id, String content) {
        CustomConfig.CustomConfigProvider configProvider =
                r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return new CustomConfig(id, "custom.txt", "custom file", content, configProvider.getProviderId());
    }

    private Config newCustomFileWithName(JenkinsRule r, String id, String name) {
        CustomConfig.CustomConfigProvider configProvider =
                r.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        return new CustomConfig(id, name, "custom file", "dummy content", configProvider.getProviderId());
    }

    private Config newMvnSettings(JenkinsRule r, String settingsId) {
        MavenSettingsConfig.MavenSettingsConfigProvider configProvider = r.jenkins
                .getExtensionList(ConfigProvider.class)
                .get(MavenSettingsConfig.MavenSettingsConfigProvider.class);
        return configProvider.newConfig(settingsId);
    }
}
