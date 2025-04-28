package org.jenkinsci.plugins.configfiles.buildwrapper;

import static org.junit.jupiter.api.Assertions.*;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.model.Cause.UserIdCause;
import hudson.tasks.Builder;
import jakarta.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig.XmlConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ConfigFileBuildWrapperTest {

    @Inject
    XmlConfigProvider xmlProvider;

    @Inject
    CustomConfigProvider customConfigProvider;

    @Test
    void envVariableMustBeAvailableInMavenModuleSetBuild(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createProject(MavenModuleSet.class);

        // p.getBuildWrappersList().add(new ConfigFileBuildWrapper(managedFiles))
        p.setMaven(ToolInstallations.configureMaven35().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize"); // -s ${MVN_SETTING}

        final Config settings = createSetting(j, "config-id", xmlProvider);
        final ManagedFile mSettings = new ManagedFile(settings.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        ParametersDefinitionProperty parametersDefinitionProperty =
                new ParametersDefinitionProperty(new StringParameterDefinition("MVN_SETTING", "/tmp/settings.xml"));
        p.addProperty(parametersDefinitionProperty);
        p.getPostbuilders().add(new VerifyEnvVariableBuilder("MVN_SETTING", "/tmp/settings.xml"));

        j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    @Test
    void envVariableMustBeReplacedInFileContent(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile =
                createCustomFile(j, "config-id", customConfigProvider, "echo ${ENV, var=\"JOB_NAME\"}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom.sh", null, true);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "echo free"));

        j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    @Test
    void envVariableMustNotBeReplacedInFileContentIfNotRequested(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile =
                createCustomFile(j, "config-id", customConfigProvider, "echo ${ENV, var=\"JOB_NAME\"}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom2.sh", null, false);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList()
                .add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "echo ${ENV, var=\"JOB_NAME\"}"));

        j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
    }

    private static final class VerifyEnvVariableBuilder extends Builder {
        private final String var, expectedValue;

        public VerifyEnvVariableBuilder(String var, String expectedValue) {
            this.var = var;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            try {
                final String expanded = TokenMacro.expandAll(build, listener, "${ENV, var=\"" + var + "\"}");
                assertEquals(expectedValue, expanded);
            } catch (MacroEvaluationException e) {
                fail("not able to expand var: " + e.getMessage());
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
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            final String fileContent = IOUtils.toString(new FileReader(filePath));
            assertEquals(expectedContent, fileContent, "file content not correct");
            return true;
        }
    }

    private Config createSetting(JenkinsRule j, String configId, ConfigProvider provider) {
        Config c1 = provider.newConfig(configId);
        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    private Config createCustomFile(JenkinsRule j, String configId, CustomConfigProvider provider, String content) {
        Config c1 = provider.newConfig(configId);
        c1 = new CustomConfig(c1.id, c1.name, c1.comment, content);
        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    @Test
    void correctConfigMustBeActiveInDropdown(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject("someJob");

        final Config activeConfig = createSetting(j, "config-id-1", xmlProvider);
        final Config secondConfig = createSetting(j, "config-id-2", xmlProvider);
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
                assertTrue(htmlOption.isSelected(), "correct config is not selected");
                foundActive = true;
            }
            if (htmlOption.getValueAttribute().equals(secondConfig.id)) {
                assertFalse(htmlOption.isSelected(), "wrong config is selected");
                foundSecond = true;
            }
        }
        assertTrue(foundActive, "configured active setting was not available as option");
        assertTrue(foundSecond, "configured second setting was not available as option");
    }
}
