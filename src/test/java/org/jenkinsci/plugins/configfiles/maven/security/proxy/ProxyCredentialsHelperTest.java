
package org.jenkinsci.plugins.configfiles.maven.security.proxy;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static com.cloudbees.plugins.credentials.CredentialsScope.SYSTEM;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProxyCredentialsHelperTest {

	final static String PASSWORD_PETER = "MY_NEW_PWD";
	final static String PASSWORD_DAN = "{COQLCE6DU6GtcS5P=}";

	@Rule
	public JenkinsRule jenkins = new JenkinsRule();

	@Test
	public void testReplaceExactlyOneProxyAuth() throws Exception {
		Map<String, StandardUsernameCredentials> proxyId2Credentials = new HashMap<>();
		proxyId2Credentials.put("proxy1", new UsernamePasswordCredentialsImpl(SYSTEM, "1", "some desc", "peter", PASSWORD_PETER));
		proxyId2Credentials.put("proxy2", new UsernamePasswordCredentialsImpl(SYSTEM, "2", "some desc", "dan", PASSWORD_DAN));
		proxyId2Credentials.put("other", new UsernamePasswordCredentialsImpl(SYSTEM, "3", "some desc", "eve", "secret"));

		final String settingsContent = IOUtils.toString(ProxyCredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));

		final String replacedContent = ProxyCredentialsHelper.fillAuthentication(settingsContent, proxyId2Credentials);

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("/settings/proxies/proxy", doc, NODESET);
		assertEquals("there is not the same number of proxy tags anymore in the settings.xml", 3, nodes.getLength());

		Node proxy1 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy1']", doc, NODE);
		assertEquals("username is not set correctly", "peter", xpath.evaluate("username", proxy1));
		assertEquals("password is not set correctly", PASSWORD_PETER, xpath.evaluate("password", proxy1));

		Node proxy2 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy2']", doc, NODE);
		assertEquals("username is not set correctly", "dan", xpath.evaluate("username", proxy2));
		assertEquals("password is not set correctly", PASSWORD_DAN, xpath.evaluate("password", proxy2));

		Node proxy3 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy3']", doc, NODE);
		assertTrue("password should not be changed", xpath.evaluate("password", proxy3).isEmpty());
		assertTrue("username should not be changed", xpath.evaluate("username", proxy3).isEmpty());
	}

	@Test
	public void testSettingsXmlIsNotChangedWithoutCredentials() throws Exception {
		Map<String, StandardUsernameCredentials> proxyId2Credentials = new HashMap<>();

		final String settingsContent = IOUtils.toString(ProxyCredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
		final String replacedContent = ProxyCredentialsHelper.fillAuthentication(settingsContent, proxyId2Credentials);

		assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);
	}

}
