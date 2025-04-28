package org.jenkinsci.plugins.configfiles.properties.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialsHelperTest {

    private static final String PWD = "bot-user-s3cr3t";

    @Test
    void testPropertiesIsReplacedWhenReplaceTrue(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put(
                "myProp",
                new UsernamePasswordCredentialsImpl(
                        CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        assertTrue(replacedContent.contains(PWD), "replaced settings_test.properties must contain new password");
    }

    @Test
    void testPropertiesIsNotReplacedWhenReplaceFalse(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put(
                "myProp",
                new UsernamePasswordCredentialsImpl(
                        CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent =
                CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        assertEquals(
                settingsContent, replacedContent, "no changes should have been made to the settings_test.properties");
    }

    @Test
    void testPropertiesIsAddedWhenReplaceTrue(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put(
                "myNewProp",
                new UsernamePasswordCredentialsImpl(
                        CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        assertTrue(replacedContent.contains(PWD), "replaced settings_test.properties must contain new password");
    }

    @Test
    void testPropertiesIsAddedWhenReplaceFalse(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();
        credentials.put(
                "myNewProp",
                new UsernamePasswordCredentialsImpl(
                        CredentialsScope.SYSTEM, "my-credentials", "some desc", "bot-user-name", PWD));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));

        final String replacedContent =
                CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        assertTrue(replacedContent.contains(PWD), "replaced settings_test.properties must contain new password");
    }

    @Test
    void testSettingsPropertiesIsNotChangedWithoutCredentialsWhenReplaceTrue(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, credentials);

        assertEquals(settingsContent, replacedContent, "no changes should have been made to the settings");
    }

    @Test
    void testSettingsPropertiesIsNotChangedWithoutCredentialsWhenReplaceFalse(JenkinsRule jenkins) throws Exception {
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.properties"));
        final String replacedContent =
                CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, credentials);

        assertEquals(settingsContent, replacedContent, "no changes should have been made to the settings");
    }
}
