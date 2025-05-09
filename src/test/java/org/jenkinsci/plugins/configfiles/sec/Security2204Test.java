package org.jenkinsci.plugins.configfiles.sec;

import static com.ibm.icu.impl.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.FilePath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.StringContains;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXParseException;

@WithJenkins
class Security2204Test {

    private static final String NEW_PASSWORD = "PASSWORD-REPLACED";

    /**
     * If we use an xml with an external entity, the parser fails to read it.
     * @throws Exception if an error happens while reading the xml or parsing it and it's not the expected one.
     */
    @Test
    @Issue("SECURITY-2204")
    void xxeOnMavenXMLDetected(JenkinsRule rr) throws Exception {
        try {
            tryToReplaceAuthOnXml(rr, "settings-xxe.xml");
        } catch (SAXParseException e) {
            assertThat(
                    e.getMessage(),
                    containsString(
                            "DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true."));
            return;
            // The console also prints the message out:
            // [Fatal Error] :5:13: External Entity: Failed to read external document 'oob.xml', because 'http' access
            // is not allowed due to restriction set by the accessExternalDTD property.
        }
        fail("The fillAuthentication should have detected the XXE but it's not the case");
    }

    /**
     * An usual maven settings.xml is correctly parsed because the parser doesn't have activated the validation by
     * default, so no need to go to the declared dtd or schema urls.
     * @throws Exception if an error happens while reading or parsing the xml.
     */
    @Test
    @Issue("SECURITY-2204")
    void mavenSettingsAuthFilled(JenkinsRule rr) throws Exception {
        String settings = "settings.xml";
        String settingsReplaced = tryToReplaceAuthOnXml(rr, settings);
        assertThat(settingsReplaced, not(StringContains.containsString("whatever-password-because-its-replaced")));
        assertThat(settingsReplaced, StringContains.containsString(NEW_PASSWORD));
    }

    private String tryToReplaceAuthOnXml(JenkinsRule rr, String file) throws Exception {
        String mavenSettingsContent = IOUtils.toString(this.getClass().getResource(file), StandardCharsets.UTF_8);
        Map<String, StandardUsernameCredentials> credentials = new HashMap<>();

        StandardUsernameCredentials usernameCredentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "credentialId", "user", "credential description", NEW_PASSWORD);
        credentials.put("credential", usernameCredentials);
        List<String> tempFiles = new ArrayList<>();
        return CredentialsHelper.fillAuthentication(
                mavenSettingsContent, true, credentials, new FilePath(rr.jenkins.root), tempFiles);
    }
}
