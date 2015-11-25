package org.jenkinsci.plugins.configfiles.buildwrapper;

import com.gargoylesoftware.htmlunit.html.*;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
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

public class ConfigFileBuildWrapperTest {

    @Rule
    public JenkinsRule          j = new JenkinsRule();

    @Inject
    ConfigFilesManagement       configManagement;

    @Inject
    MavenSettingsConfigProvider mavenSettingProvider;

    @Inject
    XmlConfigProvider           xmlProvider;

    @Test
    public void envVariableMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.createMavenProject("mvn");

        // p.getBuildWrappersList().add(new ConfigFileBuildWrapper(managedFiles))
        p.setMaven(j.configureMaven3().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("/maven3-project.zip")));
        p.setGoals("initialize"); // -s ${MVN_SETTING}

        final Config settings = createSetting(xmlProvider, "env-var-test-1");
        final ManagedFile mSettings = new ManagedFile(settings.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        ParametersDefinitionProperty parametersDefinitionProperty = new ParametersDefinitionProperty(new StringParameterDefinition("MVN_SETTING", "/tmp/settings.xml"));
        p.addProperty(parametersDefinitionProperty);
        p.getPostbuilders().add(new VerifyBuilder("MVN_SETTING", "/tmp/settings.xml"));

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

    private Config createSetting(ConfigProvider provider, String idSuffix) {
        Config c1 = provider.newConfig(idSuffix);
        provider.save(c1);
        return c1;
    }

    @Test
    public void correctConfigMustBeActiveInDropdown() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject("someJob");

        final Config activeConfig = createSetting(xmlProvider, "active-config-1");
        final Config secondConfig = createSetting(xmlProvider, "inactive-config-2");
        final Config thirdConfig = createSetting(xmlProvider, null);
        final ManagedFile mSettings = new ManagedFile(activeConfig.id, "/tmp/new_settings.xml", "MY_SETTINGS");
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mSettings));
        p.getBuildWrappersList().add(bw);

        final WebClient client = j.createWebClient();
        final HtmlPage page = client.goTo("job/someJob/configure");

        final List<HtmlElement> selectElts = page.getElementsByName("fileId");
        Assert.assertThat("Only one config file drop down list is displayed", selectElts.size(), Matchers.equalTo(1));
        HtmlSelect select = (HtmlSelect) selectElts.get(0);
        boolean foundActive = false;
        boolean foundSecond = false;

        System.err.println("BEGIN Evaluate options against activeConfig: " + activeConfig + ", secondConfig: " + secondConfig);
        for (HtmlOption htmlOption : select.getOptions()) {
            System.err.println("Evaluate option value=" + htmlOption.getValueAttribute() + ", text=" + htmlOption.getText() + ", selected=" + htmlOption.isSelected());
            if (htmlOption.getValueAttribute().equals(activeConfig.id)) {
                Assert.assertTrue("correct config ('" + htmlOption.getValueAttribute() + "') is not selected", htmlOption.isSelected());
                foundActive = true;
            }
            if (htmlOption.getValueAttribute().equals(secondConfig.id)) {
                Assert.assertFalse("wrong config ('" + htmlOption.getValueAttribute() + "') is selected", htmlOption.isSelected());
                foundSecond = true;
            }
        }
        System.err.println("END Evaluate options");
        Assert.assertTrue("configured active setting was not available as option", foundActive);
        Assert.assertTrue("configured second setting was not available as option", foundSecond);
    }
}
