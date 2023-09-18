package org.jenkinsci.plugins.configfiles.properties.security;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.HashMap;
import java.util.Map;

public class CredentialsHelperTest {

    private final static String PWD = "bot-user-s3cr3t";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testPropertiesIsReplacedWhenReplaceTrue() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put("myProp", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        Assert.assertTrue("replaced settings_test.properties must contain new password", replacedContent.contains(PWD));
    }

    @Test
    public void testPropertiesIsNotReplacedWhenReplaceFalse() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put("myProp", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        Assert.assertEquals("no changes should have been made to the settings_test.properties", settingsContent, replacedContent);
    }

    @Test
    public void testPropertiesIsAddedWhenReplaceTrue() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put("myNewProp", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        Assert.assertTrue("replaced settings_test.properties must contain new password", replacedContent.contains(PWD));
    }

    @Test
    public void testPropertiesIsAddedWhenReplaceFalse() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put("myNewProp", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        Assert.assertTrue("replaced settings_test.properties must contain new password", replacedContent.contains(PWD));
    }

    @Test
    public void testSettingsPropertiesIsNotChangedWithoutCredentialsWhenReplaceTrue() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);
    }

    @Test
    public void testSettingsPropertiesIsNotChangedWithoutCredentialsWhenReplaceFalse() throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);
    }

}
