package org.jenkinsci.plugins.configfiles.maven.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@WithJenkins
class CredentialsHelperTest {

    static final String PWD = "MY_NEW_PWD";
    static final String PWD_2 = "{COQLCE6DU6GtcS5P=}";
    static final int SERVER_COUNT = 5;

    @Test
    void testIfServerAuthIsReplacedWithinSettingsXmlWhenReplaceTrue(JenkinsRule jenkins) throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<>();
        serverId2Credentials.put(
                "my.server",
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));
        serverId2Credentials.put(
                "encoded_pwd",
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "*pwd", "some desc2", "dan", PWD_2));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));

        List<String> tempFiles = new ArrayList<>();
        final String replacedContent = CredentialsHelper.fillAuthentication(
                settingsContent, Boolean.TRUE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        assertTrue(replacedContent.contains(PWD), "replaced settings.xml must contain new password");

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        // the 'settings_test.xml' actually contains 4 server tags, but we remove all and only inject the ones managed
        // by the plugin
        assertEquals(2, nodes.getLength(), "there is not the same number of server tags anymore in the settings.xml");

        Node server = (Node) xpath.evaluate("/settings/servers/server[id='my.server']", doc, XPathConstants.NODE);
        assertEquals(PWD, xpath.evaluate("password", server), "password is not at the correct location");
        assertEquals("peter", xpath.evaluate("username", server), "username is not set correct");

        Node server2 = (Node) xpath.evaluate("/settings/servers/server[id='encoded_pwd']", doc, XPathConstants.NODE);
        assertEquals(PWD_2, xpath.evaluate("password", server2), "password is not set correct");
        assertEquals("dan", xpath.evaluate("username", server2), "username is not set correct");
    }

    @Test
    void testIfServerAuthIsReplacedWithinSettingsXmlWhenReplaceFalse(JenkinsRule jenkins) throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<>();
        serverId2Credentials.put(
                "my.server",
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));
        serverId2Credentials.put(
                "encoded_pwd",
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "*pwd", "some desc2", "dan", PWD_2));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<>();
        final String replacedContent = CredentialsHelper.fillAuthentication(
                settingsContent, Boolean.FALSE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        assertTrue(replacedContent.contains(PWD), "replaced settings.xml must contain new password");

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        // the 'settings_test.xml' actually contains 4 server tags, but we remove all and only inject the ones managed
        // by the plugin
        assertEquals(
                SERVER_COUNT,
                nodes.getLength(),
                "there is not the same number of server tags anymore in the settings.xml");
    }

    @Test
    void testSettingsXmlIsNotChangedWithoutCredentialsWhenReplaceTrue(JenkinsRule jenkins) throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<>();

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<>();
        final String replacedContent = CredentialsHelper.fillAuthentication(
                settingsContent, Boolean.TRUE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        assertEquals(settingsContent, replacedContent, "no changes should have been made to the settings");
    }

    @Test
    void testSettingsXmlIsNotChangedWithoutCredentialsWhenReplaceFalse(JenkinsRule jenkins) throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<>();

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<>();
        final String replacedContent = CredentialsHelper.fillAuthentication(
                settingsContent, Boolean.FALSE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        assertEquals(settingsContent, replacedContent, "no changes should have been made to the settings");
    }

    @Issue("JENKINS-39991")
    @Test
    void testIfServerElementAreKeptWhenMatchCredentialsWhenReplaceFalse(JenkinsRule jenkins) throws Exception {
        testIfServerElementAreKeptWhenMatchCredentials(jenkins, false);
    }

    @Issue("JENKINS-39991")
    @Test
    void testIfServerElementAreKeptWhenMatchCredentialsWhenReplaceTrue(JenkinsRule jenkins) throws Exception {
        testIfServerElementAreKeptWhenMatchCredentials(jenkins, true);
    }

    private void testIfServerElementAreKeptWhenMatchCredentials(JenkinsRule jenkins, boolean replaceAll)
            throws Exception {
        final String serverId = "jenkins-39991";

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<>();
        serverId2Credentials.put(
                serverId,
                new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));

        final String settingsContent =
                IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<>();
        final String replacedContent = CredentialsHelper.fillAuthentication(
                settingsContent, replaceAll, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        // read original server settings
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(settingsContent)));
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node server =
                (Node) xpath.evaluate("/settings/servers/server[id='" + serverId + "']", doc, XPathConstants.NODE);
        String filePermissions = xpath.evaluate("filePermissions", server);
        String directoryPermissions = xpath.evaluate("directoryPermissions", server);
        String configuration = xpath.evaluate("configuration", server);

        // ensure it is still a valid XML document
        doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        xpath = XPathFactory.newInstance().newXPath();

        server = (Node) xpath.evaluate("/settings/servers/server[id='" + serverId + "']", doc, XPathConstants.NODE);
        assertEquals(PWD, xpath.evaluate("password", server), "password is not at the correct location");
        assertEquals("peter", xpath.evaluate("username", server), "username is not set correct");
        assertEquals(filePermissions, xpath.evaluate("filePermissions", server), "filePermissions is not set correct");
        assertEquals(
                directoryPermissions,
                xpath.evaluate("directoryPermissions", server),
                "directoryPermissions is not set correct");
        assertEquals(
                configuration.trim(),
                xpath.evaluate("configuration", server).trim(),
                "configuration is not set correct");
    }
}
