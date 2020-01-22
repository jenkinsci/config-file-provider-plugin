package org.jenkinsci.plugins.configfiles.buildwrapper;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.model.Cause.UserIdCause;
import hudson.tasks.Builder;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig.XmlConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.ToolInstallations;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

public class ConfigFileBuildWrapperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public BuildWatcher buildWatcher = new BuildWatcher();

    @Inject
    ConfigFilesManagement configManagement;

    @Inject
    MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    XmlConfigProvider xmlProvider;

    @Inject
    CustomConfigProvider customConfigProvider;

    @Inject
    ConfigFilesManagement configFilesManagement;

    @Test
    public void envVariableMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createProject(MavenModuleSet.class);

        // p.getBuildWrappersList().add(new ConfigFileBuildWrapper(managedFiles))
        p.setMaven(ToolInstallations.configureMaven35().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize"); // -s ${MVN_SETTING}

        final Config settings = createSetting("config-id", xmlProvider);
        final ManagedFile mSettings = new ManagedFile(settings.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        ParametersDefinitionProperty parametersDefinitionProperty = new ParametersDefinitionProperty(new StringParameterDefinition("MVN_SETTING", "/tmp/settings.xml"));
        p.addProperty(parametersDefinitionProperty);
        p.getPostbuilders().add(new VerifyEnvVariableBuilder("MVN_SETTING", "/tmp/settings.xml"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    @Test
    public void envVariableMustBeReplacedInFileContent() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile = createCustomFile("config-id", customConfigProvider, "echo ${ENV, var=\"JOB_NAME\"}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom.sh", null, true);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "echo free"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    @Test
    public void envVariableMustNotBeReplacedInFileContentIfNotRequested() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile = createCustomFile("config-id", customConfigProvider, "echo ${ENV, var=\"JOB_NAME\"}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom2.sh", null, false);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "echo ${ENV, var=\"JOB_NAME\"}"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    private static final class VerifyEnvVariableBuilder extends Builder {
        private final String var, expectedValue;

        public VerifyEnvVariableBuilder(String var, String expectedValue) {
            this.var = var;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            try {
                final String expanded = TokenMacro.expandAll(build, listener, "${ENV, var=\"" + var + "\"}");
                Assert.assertEquals(expectedValue, expanded);
            } catch (MacroEvaluationException e) {
                Assert.fail("not able to expand var: " + e.getMessage());
            }
            return true;
        }
    }

    private static final class VerifyFileContentBuilder extends Builder {
        private final String filePath;
        private final String expectedContent;

        public VerifyFileContentBuilder(String filePath, String expectedContent) {
            this.filePath = filePath;
            this.expectedContent = expectedContent;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            final String fileContent = IOUtils.toString(new FileReader(filePath));
            Assert.assertEquals("file content not correct", expectedContent, fileContent);
            return true;
        }
    }

    private Config createSetting(String configId, ConfigProvider provider) {
        Config c1 = provider.newConfig(configId);
        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    private Config createCustomFile(String configId, CustomConfigProvider provider, String content) {
        Config c1 = provider.newConfig(configId);
        c1 = new CustomConfig(c1.id, c1.name, c1.comment, content);
        GlobalConfigFiles globalConfigFiles = j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    @Test
    public void correctConfigMustBeActiveInDropdown() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject("someJob");

        final Config activeConfig = createSetting("config-id-1", xmlProvider);
        final Config secondConfig = createSetting("config-id-2", xmlProvider);
        final ManagedFile mSettings = new ManagedFile(activeConfig.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        final WebClient client = j.createWebClient();
        final HtmlPage page = client.goTo("job/someJob/configure");

        final DomNodeList<DomElement> option = page.getElementsByTagName("option");
        boolean foundActive = false;
        boolean foundSecond = false;
        for (DomElement htmlElement : option) {
            final HtmlOption htmlOption = (HtmlOption) htmlElement;
            if (htmlOption.getValueAttribute().equals(activeConfig.id)) {
                Assert.assertTrue("correct config is not selected", htmlOption.isSelected());
                foundActive = true;
            }
            if (htmlOption.getValueAttribute().equals(secondConfig.id)) {
                Assert.assertFalse("wrong config is selected", htmlOption.isSelected());
                foundSecond = true;
            }
        }
        Assert.assertTrue("configured active setting was not available as option", foundActive);
        Assert.assertTrue("configured second setting was not available as option", foundSecond);
    }
}
