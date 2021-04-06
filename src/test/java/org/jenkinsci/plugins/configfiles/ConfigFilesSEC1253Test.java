package org.jenkinsci.plugins.configfiles;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfigFilesSEC1253Test {

    private static final String CONFIG_ID = "ConfigFilesTestId";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("SECURITY-1253")
    public void regularCaseStillWorking() throws Exception {
        GlobalConfigFiles store = j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        assertNotNull(store);
        assertThat(store.getConfigs(), empty());

        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage createConfig = wc.goTo("configfiles/selectProvider");
        HtmlRadioButtonInput groovyRadioButton = createConfig.getDocumentElement().getOneHtmlElementByAttribute("input", "value", "org.jenkinsci.plugins.configfiles.groovy.GroovyScript");
        groovyRadioButton.click();
        HtmlForm addConfigForm = createConfig.getFormByName("addConfig");

        HtmlPage createGroovyConfig = j.submit(addConfigForm);
        HtmlInput configIdInput = createGroovyConfig.getElementByName("config.id");
        configIdInput.setValueAttribute(CONFIG_ID);

        HtmlInput configNameInput = createGroovyConfig.getElementByName("config.name");
        configNameInput.setValueAttribute("Regular name");

        HtmlForm saveConfigForm = createGroovyConfig.getForms().stream()
                .filter(htmlForm -> htmlForm.getActionAttribute().equals("saveConfig"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No config with action [saveConfig] in that page"));
        j.submit(saveConfigForm);

        assertThat(store.getConfigs(), hasSize(1));

        HtmlPage configFiles = wc.goTo("configfiles");
        HtmlAnchor removeAnchor = configFiles.getDocumentElement().getFirstByXPath("//a[contains(@onclick, 'removeConfig?id=" + CONFIG_ID + "')]");

        AtomicReference<Boolean> confirmCalled = new AtomicReference<>(false);
        wc.setConfirmHandler((page, s) -> {
            confirmCalled.set(true);
            return true;
        });

        assertThat(confirmCalled.get(), is(false));

        removeAnchor.click();

        assertThat(confirmCalled.get(), is(true));

        assertThat(store.getConfigs(), empty());
    }

    @Test
    @Issue("SECURITY-1253")
    public void xssPrevention() throws Exception {
        GlobalConfigFiles store = j.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        assertNotNull(store);
        assertThat(store.getConfigs(), empty());

        CustomConfig config = new CustomConfig(CONFIG_ID, "GroovyConfig')+alert('asw", "comment", "content");
        store.save(config);

        assertThat(store.getConfigs(), hasSize(1));

        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage configFiles = wc.goTo("configfiles");
        HtmlAnchor removeAnchor = configFiles.getDocumentElement().getFirstByXPath("//a[contains(@onclick, 'removeConfig?id=" + CONFIG_ID + "')]");

        AtomicReference<Boolean> confirmCalled = new AtomicReference<>(false);
        AtomicReference<Boolean> alertCalled = new AtomicReference<>(false);
        wc.setConfirmHandler((page, s) -> {
            confirmCalled.set(true);
            return true;
        });
        wc.setAlertHandler((page, s) -> {
            alertCalled.set(true);
        });

        assertThat(confirmCalled.get(), is(false));
        assertThat(alertCalled.get(), is(false));

        removeAnchor.click();

        assertThat(confirmCalled.get(), is(true));
        assertThat(alertCalled.get(), is(false));

        assertThat(store.getConfigs(), empty());
    }
}
