
package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class CredentialsHelper {

    private final static Logger LOGGER = Logger.getLogger(CredentialsHelper.class.getName());

    /**
     * hide constructor
     */
    private CredentialsHelper() {
    }

    /**
     * Resolves the given serverCredential mappings and returns a map paring serverId to credential
     * 
     * @param item
     *            authentication scope
     * @param serverCredentialMappings
     *            the mappings to be resolved
     * @return map of serverId - credential
     */
    public static Map<String, StandardUsernameCredentials> resolveCredentials(Run<?,?> build, final List<ServerCredentialMapping> serverCredentialMappings) {
        Map<String, StandardUsernameCredentials> serverId2credential = new HashMap<String, StandardUsernameCredentials>();
        for (ServerCredentialMapping serverCredentialMapping : serverCredentialMappings) {
            final String credentialsId = serverCredentialMapping.getCredentialsId();
            final String serverId = serverCredentialMapping.getServerId();
            
            List<DomainRequirement> domainRequirements = Collections.emptyList();
            if (StringUtils.isNotBlank(serverId)) {
                domainRequirements = Collections.<DomainRequirement> singletonList(new MavenServerIdRequirement(serverId));
            }

            final StandardUsernameCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernameCredentials.class, build, domainRequirements);

            if (c != null) {
                serverId2credential.put(serverId, c);
            }
        }
        return serverId2credential;
    }

    /**
     * 
     * @param settingsContent
     *            settings xml (must be valid settings XML)
     * @param serverId2credential
     *            the credentials to be inserted into the XML
     * @return the new XML with the server credentials added
     * @throws Exception
     */
    public static String fillAuthentication(String settingsContent, final Boolean isReplaceAll, Map<String, StandardUsernameCredentials> serverId2credential) throws Exception {
        String content = settingsContent;

        if (!serverId2credential.isEmpty()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(content)));

            final Set<Entry<String, StandardUsernameCredentials>> credentialEntries = serverId2credential.entrySet();

            // locate the server node(s)
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node serversNode = (Node) xpath.evaluate("/settings/servers", doc, XPathConstants.NODE);
            if (serversNode == null) {
                // need to create a 'servers' node
                Node settingsNode = (Node) xpath.evaluate("/settings", doc, XPathConstants.NODE);
                serversNode = doc.createElement("servers");
                settingsNode.appendChild(serversNode);
            } else {
                // remove all server nodes, we will replace every single one and only add the ones provided
                removeAllChilds(serversNode, serverId2credential.keySet(), isReplaceAll);
            }

            for (Entry<String, StandardUsernameCredentials> srvId2credential : credentialEntries) {

                final StandardUsernameCredentials credential = srvId2credential.getValue();
                if (credential instanceof StandardUsernamePasswordCredentials) {

                    StandardUsernamePasswordCredentials userPwd = (StandardUsernamePasswordCredentials) credential;
                    LOGGER.fine("add: " + srvId2credential.getKey() + " -> " + userPwd);

                    final Element server = doc.createElement("server");

                    // create and add the relevant xml elements
                    final Element id = doc.createElement("id");
                    id.setTextContent(srvId2credential.getKey());
                    final Element password = doc.createElement("password");
                    password.setTextContent(Secret.toString(userPwd.getPassword()));
                    final Element username = doc.createElement("username");
                    username.setTextContent(userPwd.getUsername());

                    server.appendChild(id);
                    server.appendChild(username);
                    server.appendChild(password);

                    serversNode.appendChild(server);
                } else {

                    Object[] params = new Object[] { srvId2credential.getKey(), credential.getClass() };
                    LOGGER.log(Level.SEVERE, "credentials for {0} of type {1} not (yet) supported", params);

                }

            }

            // save the result
            StringWriter writer = new StringWriter();
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(writer));
            content = writer.toString();
        }
        return content;
    }

    /**
     * Removes all childs
     * 
     * @param serversNode
     *            the node to remove all childs from
     */
    private static void removeAllChilds(final Node serversNode, final Set<String> credentialKeys, final Boolean replaceAll) {
        final NodeList serverNodes = serversNode.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            final Node server = serverNodes.item(i);
            String serverId = getServerId(server);
            if (Boolean.TRUE.equals(replaceAll) || (credentialKeys.contains(serverId))) {
                serversNode.removeChild(server);
                --i;
            }
        }
    }

    private static String getServerId(Node server) {
        NodeList nodes = server.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = node.getNodeName();
            String content = node.getTextContent();
            if ("id" == name.toLowerCase()) {
                return content;
            }
        }
        return null;
    }

}
