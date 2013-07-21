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

public class CredentialsHelperTest {

    final static String PWD = "MY_NEW_PWD";
    final static String PWD_2 = "{COQLCE6DU6GtcS5P=}";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testIfServerAuthIsReplacedWithinSettginsXml() throws Exception {

        Map<String, BaseMvnServerCredentials> credentials = new HashMap<String, BaseMvnServerCredentials>();
        credentials.put("my.server", new MvnServerPassword(CredentialsScope.SYSTEM, "my.server", "peter", PWD, "some desc"));
        credentials.put("encoded_pwd", new MvnServerPassword(CredentialsScope.SYSTEM, "encoded_pwd", "dan", PWD_2, "some desc"));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, credentials);

        Assert.assertTrue("replaced settings.xml must contain new password", replacedContent.contains(PWD));

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        Assert.assertEquals("there is not the same number of server tags anymore in the settgins.xml", 4, nodes.getLength());

        Node server = (Node) xpath.evaluate("/settings/servers/server[id='my.server']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not at the correct location", PWD, xpath.evaluate("password", server));
        Assert.assertEquals("username is not set correct", "peter", xpath.evaluate("username", server));

        Node server2 = (Node) xpath.evaluate("/settings/servers/server[id='encoded_pwd']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not set correct", PWD_2, xpath.evaluate("password", server2));
        Assert.assertEquals("username is not set correct", "dan", xpath.evaluate("username", server2));
    }

    @Test
    public void testSettignsXmlIsNotChangedWithoutCredentials() throws Exception {

        Map<String, BaseMvnServerCredentials> credentials = new HashMap<String, BaseMvnServerCredentials>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, credentials);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);

    }
}
