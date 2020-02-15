
package org.jenkinsci.plugins.configfiles.maven.security.proxy;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;

public class ProxyCredentialsHelper {

	private static final Logger LOGGER = Logger.getLogger(ProxyCredentialsHelper.class.getName());

	/**
	 * hide constructor
	 */
	private ProxyCredentialsHelper() {
	}

	/**
	 * Resolves the given proxyCredential mappings and returns a map paring proxyId to credential
	 *
	 * @param build                   authentication scope
	 * @param proxyCredentialMappings the mappings to be resolved
	 * @param listener                the listener
	 * @return map of proxyId - credential
	 */
	public static Map<String, StandardUsernameCredentials> resolveCredentials(Run<?, ?> build, final List<ProxyCredentialMapping> proxyCredentialMappings, TaskListener listener) {
		LOGGER.entering(ProxyCredentialsHelper.class.getName(), "resolveProxyCredentials()");

		Map<String, StandardUsernameCredentials> proxyId2credential = new HashMap<>();
		for (ProxyCredentialMapping proxyCredentialMapping : proxyCredentialMappings) {
			final String credentialsId = proxyCredentialMapping.getCredentialsId();
			final String proxyId = proxyCredentialMapping.getProxyId();

			List<DomainRequirement> domainRequirements = Collections.emptyList();
			if (StringUtils.isNotBlank(proxyId)) {
				domainRequirements = Collections.singletonList(new MavenProxyIdRequirement(proxyId));
			}

			final StandardUsernameCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernameCredentials.class, build, domainRequirements);

			if (c != null) {
				proxyId2credential.put(proxyId, c);
			} else {
				listener.getLogger().println("Could not find credentials [" + credentialsId + "] for " + build);
			}
		}
		return proxyId2credential;
	}

	public static String fillAuthentication(
			String mavenSettingsContent, Map<String, StandardUsernameCredentials> mavenProxyId2jenkinsCredential) throws Exception {
		LOGGER.entering(ProxyCredentialsHelper.class.getName(), "fillAuthentication()");

		if (mavenProxyId2jenkinsCredential.isEmpty())
			return mavenSettingsContent;

		InputSource inputSource = new InputSource(new StringReader(mavenSettingsContent));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource);
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList proxyNodes = (NodeList) xpath.evaluate("/settings/proxies/proxy", doc, NODESET);
		for (int i = 0; i < proxyNodes.getLength(); i++) {
			Node proxyNode = proxyNodes.item(i);

			Node proxyIdNode = (Node) xpath.evaluate("id", proxyNode, NODE);
			if (proxyIdNode == null) continue;

			String proxyId = proxyIdNode.getTextContent();

			if (mavenProxyId2jenkinsCredential.containsKey(proxyId)) {
				StandardUsernameCredentials usernameCredentials = mavenProxyId2jenkinsCredential.get(proxyId);

				if (usernameCredentials instanceof StandardUsernamePasswordCredentials) {
					StandardUsernamePasswordCredentials usernamePasswordCredentials = (StandardUsernamePasswordCredentials) usernameCredentials;

					Node proxyUsernameNode = getOrCreateNode(doc, xpath, proxyNode, "username");
					proxyUsernameNode.setTextContent(usernamePasswordCredentials.getUsername());

					Node proxyPasswordNode = getOrCreateNode(doc, xpath, proxyNode, "password");
					proxyPasswordNode.setTextContent(Secret.toString(usernamePasswordCredentials.getPassword()));

					LOGGER.fine(() -> format("Replace proxy credentials in settings file for proxy-id %s", proxyId));
				}
			}
		}

		StringWriter writer = new StringWriter();
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		xformer.transform(new DOMSource(doc), new StreamResult(writer));
		return writer.toString();
	}

	private static Node getOrCreateNode(Document doc, XPath xpath, Node proxyNode, String username) throws XPathExpressionException {
		Node proxyUsernameNode = (Node) xpath.evaluate(username, proxyNode, NODE);
		if (proxyUsernameNode == null) {
			proxyUsernameNode = doc.createElement(username);
			proxyNode.appendChild(proxyUsernameNode);
		}
		return proxyUsernameNode;
	}

}