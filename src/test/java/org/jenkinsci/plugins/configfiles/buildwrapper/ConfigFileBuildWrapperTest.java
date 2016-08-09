package org.jenkinsci.plugins.configfiles.buildwrapper;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;

import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig.XmlConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.Cause.UserCause;
import hudson.tasks.Builder;

public class ConfigFileBuildWrapperTest {

    @Rule
    public JenkinsRule          j = new JenkinsRule();

    @Inject
    ConfigFilesManagement       configManagement;

    @Inject
    MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    XmlConfigProvider           xmlProvider;

    @Inject
    CustomConfigProvider        customConfigProvider;

    @Inject
    ConfigFilesManagement       configFilesManagement;

    @Test
    public void envVariableMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createMavenProject("mvn");

        // p.getBuildWrappersList().add(new ConfigFileBuildWrapper(managedFiles))
        p.setMaven(j.configureMaven3().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize"); // -s ${MVN_SETTING}

        final Config settings = createSetting(xmlProvider);
        final ManagedFile mSettings = new ManagedFile(settings.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        ParametersDefinitionProperty parametersDefinitionProperty = new ParametersDefinitionProperty(new StringParameterDefinition("MVN_SETTING", "/tmp/settings.xml"));
        p.addProperty(parametersDefinitionProperty);
        p.getPostbuilders().add(new VerifyBuilder("MVN_SETTING", "/tmp/settings.xml"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }

    @Test
    public void envVariableMustBeReplacedInFileContent() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile = createCustomFile(customConfigProvider, "echo ${USER}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom.sh", null, true);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        final String userName = System.getProperty("user.name");
        p.getBuildersList().add(new VerifyFileBuilder(mCustom.targetLocation, "echo " + userName));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }

    @Test
    public void envVariableMustNotBeReplacedInFileContentIfNotRequested() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final Config customFile = createCustomFile(customConfigProvider, "echo ${USER}");

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(customFile.id, "/tmp/new_custom2.sh", null, false);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileBuilder(mCustom.targetLocation, "echo ${USER}"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }

    private static final class VerifyBuilder extends Builder {
        private final String var, expectedValue;

        public VerifyBuilder(String var, String expectedValue) {
            this.var = var;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            try {
                final String expanded = TokenMacro.expandAll(build, listener, "${ENV, var=\"" + var + "\"}");
                System.out.println("-->" + expanded);
                Assert.assertEquals(expectedValue, expanded);
            } catch (MacroEvaluationException e) {
                Assert.fail("not able to expand var: " + e.getMessage());
            }
            return true;
        }
    }

    private static final class VerifyFileBuilder extends Builder {
        private final String filePath;
        private final String expectedContent;

        public VerifyFileBuilder(String filePath, String expectedContent) {
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

    private Config createSetting(ConfigProvider provider) {
        Config c1 = provider.newConfig();
        provider.save(c1);
        return c1;
    }

    private Config createCustomFile(CustomConfigProvider provider, String content) {
        Config c1 = provider.newConfig();
        c1 = new CustomConfig(c1.id, c1.name, c1.comment, content);
        provider.save(c1);
        return c1;
    }

    @Test
    public void correctConfigMustBeActiveInDropdown() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject("someJob");

        final Config activeConfig = createSetting(xmlProvider);
        final Config secondConfig = createSetting(xmlProvider);
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
