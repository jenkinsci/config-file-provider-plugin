package org.jenkinsci.plugins.configfiles.maven.security;

import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;

import java.io.StringReader;
import java.io.StringWriter;
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

import jenkins.model.Jenkins;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

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
    public static Map<String, StandardUsernameCredentials> resolveCredentials(Item item, final List<ServerCredentialMapping> serverCredentialMappings) {
        Map<String, StandardUsernameCredentials> serverId2credential = new HashMap<String, StandardUsernameCredentials>();
        for (ServerCredentialMapping serverCredentialMapping : serverCredentialMappings) {
            final String credentialsId = serverCredentialMapping.getCredentialsId();
            final String serverId = serverCredentialMapping.getServerId();
            final List<StandardUsernameCredentials> foundCredentials = findValidCredentials(serverId);
            final StandardUsernameCredentials c = CredentialsMatchers.firstOrNull(foundCredentials, CredentialsMatchers.withId(credentialsId));
            if (c != null) {
                serverId2credential.put(serverId, c);
            }
        }
        return serverId2credential;
    }

    public static List<StandardUsernameCredentials> findValidCredentials(final String serverIdPattern) {
        final List<StandardUsernameCredentials> foundCredentials = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, /** TODO? item **/
        Jenkins.getInstance(), ACL.SYSTEM, new MavenServerIdRequirement(serverIdPattern));
        return foundCredentials;
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
    public static String fillAuthentication(String settingsContent, Map<String, StandardUsernameCredentials> serverId2credential) throws Exception {
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
                removeAllChilds(serversNode);
            }

            for (Entry<String, StandardUsernameCredentials> srvId2credential : credentialEntries) {

                final StandardUsernameCredentials credential = srvId2credential.getValue();
                if (credential instanceof StandardUsernamePasswordCredentials) {

                    StandardUsernamePasswordCredentials userPwd = (StandardUsernamePasswordCredentials) credential;
                    System.out.println("add: " + srvId2credential.getKey() + " -> " + userPwd);

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
                    LOGGER.log(Level.SEVERE, "credentials for {0} of type {1} not supported", params);

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
     * @param serverNode
     *            the node to remove all childs from
     */
    private static void removeAllChilds(final Node serverNode) {
        final NodeList childNodes = serverNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node child = childNodes.item(i);
            serverNode.removeChild(child);
            --i;
        }

    }

}
