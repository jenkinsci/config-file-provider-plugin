package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class CredentialsHelperTest {

    final static String PWD     = "MY_NEW_PWD";
    final static String PWD_2   = "{COQLCE6DU6GtcS5P=}";

    @Rule
    public JenkinsRule  jenkins = new JenkinsRule();

    @Test
    public void testIfServerAuthIsReplacedWithinSettginsXml() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();
        serverId2Credentials.put("my.server", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));
        serverId2Credentials.put("encoded_pwd", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "*pwd", "some desc2", "dan", PWD_2));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, serverId2Credentials);

        Assert.assertTrue("replaced settings.xml must contain new password", replacedContent.contains(PWD));

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        // the 'settings_test.xml' actually contains 4 server tags, but we remove all and only inject the ones managed by the plugin
        Assert.assertEquals("there is not the same number of server tags anymore in the settgins.xml", 2, nodes.getLength());

        Node server = (Node) xpath.evaluate("/settings/servers/server[id='my.server']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not at the correct location", PWD, xpath.evaluate("password", server));
        Assert.assertEquals("username is not set correct", "peter", xpath.evaluate("username", server));

        Node server2 = (Node) xpath.evaluate("/settings/servers/server[id='encoded_pwd']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not set correct", PWD_2, xpath.evaluate("password", server2));
        Assert.assertEquals("username is not set correct", "dan", xpath.evaluate("username", server2));

    }

    @Test
    public void testSettignsXmlIsNotChangedWithoutCredentials() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, serverId2Credentials);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);

    }
}
