
package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
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

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;

public class CredentialsHelper {

    private static final Logger LOGGER = Logger.getLogger(CredentialsHelper.class.getName());
    private static final Collection<String> ATTRIBUTES_TO_KEEP = Arrays.asList("filePermissions", "directoryPermissions", "configuration");

    /**
     * hide constructor
     */
    private CredentialsHelper() {
    }

    /**
     * Resolves the given serverCredential mappings and returns a map paring serverId to credential
     *
     * @param build
     *            authentication scope
     * @param serverCredentialMappings
     *            the mappings to be resolved
     * @param listener
     *            the listener
     * @return map of serverId - credential
     */
    public static Map<String, StandardUsernameCredentials> resolveCredentials(Run<?,?> build, final List<ServerCredentialMapping> serverCredentialMappings, TaskListener listener) {
        Map<String, StandardUsernameCredentials> serverId2credential = new HashMap<String, StandardUsernameCredentials>();
        for (ServerCredentialMapping serverCredentialMapping : serverCredentialMappings) {
            final String credentialsId = serverCredentialMapping.getCredentialsId();
            final String serverId = serverCredentialMapping.getServerId();
            
            List<DomainRequirement> domainRequirements = Collections.emptyList();
            if (StringUtils.isNotBlank(serverId)) {
                domainRequirements = Collections.singletonList(new MavenServerIdRequirement(serverId));
            }

            final StandardUsernameCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernameCredentials.class, build, domainRequirements);

            if (c != null) {
                serverId2credential.put(serverId, c);
            } else {
                listener.getLogger().println("Could not find credentials [" + credentialsId + "] for " + build);
            }
        }
        return serverId2credential;
    }

    @Deprecated
    public static Map<String, StandardUsernameCredentials> resolveCredentials(Run<?,?> build, final List<ServerCredentialMapping> serverCredentialMappings) {
        return resolveCredentials(build, serverCredentialMappings, TaskListener.NULL);
    }

    /**
     * 
     * @param mavenSettingsContent
     *            Maven settings.xml (must be valid XML)
     * @param mavenServerId2jenkinsCredential
     *            the credentials to be inserted into the XML (key: Maven serverId, value: Jenkins credentials)
     * @param isReplaceAllServerDefinitions overwrite all the {@code <server>} declarations. If {@code false}, only the
     *            {@code <server>} with an {@code id} matching the given {@code mavenServerId2jenkinsCredential} are overwritten.
     * @param workDir
     *            folder in which credentials files are created if needed (private key files...)
     * @param tempFiles
     *            temp files created by this method, these files MUST be deleted by the caller
     * @return the updated version of the {@code mavenSettingsContent} with the server credentials added
     * @throws Exception
     */
    public static String fillAuthentication(String mavenSettingsContent, final Boolean isReplaceAllServerDefinitions,
                                            Map<String, StandardUsernameCredentials> mavenServerId2jenkinsCredential,
                                            FilePath workDir, List<String> tempFiles) throws Exception {
        String content = mavenSettingsContent;

        if (mavenServerId2jenkinsCredential.isEmpty()) {
            return mavenSettingsContent;
        }

        // TODO: switch to XMLUtils.parse(Reader) when the baseline > 2.179 or  XMLUtils.parse(InputSteam) > 2.265
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        //documentBuilderFactory.isValidating() is false by default, so these attributes won't avoid to parse an usual maven settings.
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document doc = documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(content)));

        Map<String, Node> removedMavenServers = Collections.emptyMap();

        // locate the server node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node serversNode = (Node) xpath.evaluate("/settings/servers", doc, XPathConstants.NODE);
        if (serversNode == null) {
            // need to create a 'servers' node
            Node settingsNode = (Node) xpath.evaluate("/settings", doc, XPathConstants.NODE);
            serversNode = doc.createElement("servers");
            settingsNode.appendChild(serversNode);
        } else {
            // remove the server nodes
        	removedMavenServers = removeMavenServerDefinitions(serversNode, mavenServerId2jenkinsCredential.keySet(), Boolean.TRUE.equals(isReplaceAllServerDefinitions));
        }

        for (Entry<String, StandardUsernameCredentials> mavenServerId2JenkinsCredential : mavenServerId2jenkinsCredential.entrySet()) {

            final StandardUsernameCredentials credential = mavenServerId2JenkinsCredential.getValue();
            String mavenServerId = mavenServerId2JenkinsCredential.getKey();

            Node currentDefinition = removedMavenServers.get(mavenServerId);
            if (credential instanceof StandardUsernamePasswordCredentials) {

                StandardUsernamePasswordCredentials usernamePasswordCredentials = (StandardUsernamePasswordCredentials) credential;
                LOGGER.log(Level.FINE, "Maven Server ID {0}: use {1} / {2}", new Object[]{mavenServerId, usernamePasswordCredentials.getId(), usernamePasswordCredentials.getDescription()});

                final Element server = doc.createElement("server");

                // create and add the relevant xml elements
                final Element id = doc.createElement("id");
                id.setTextContent(mavenServerId);
                final Element password = doc.createElement("password");
                password.setTextContent(Secret.toString(usernamePasswordCredentials.getPassword()));
                final Element username = doc.createElement("username");
                username.setTextContent(usernamePasswordCredentials.getUsername());

                server.appendChild(id);
                server.appendChild(username);
                server.appendChild(password);
                copyServerAttributes(currentDefinition,	server);

                serversNode.appendChild(server);
            } else if (credential instanceof SSHUserPrivateKey) {
                SSHUserPrivateKey sshUserPrivateKey = (SSHUserPrivateKey) credential;
                List<String> privateKeys = sshUserPrivateKey.getPrivateKeys();
                String privateKeyContent;

                if (privateKeys.isEmpty()) {
                    LOGGER.log(Level.WARNING, "Maven Server ID {0}: not private key defined in {1}, skip", new Object[]{mavenServerId, sshUserPrivateKey.getId()});
                    continue;
                } else if (privateKeys.size() == 1) {
                    LOGGER.log(Level.FINE, "Maven Server ID {0}: use {1}", new Object[]{mavenServerId, sshUserPrivateKey.getId()});
                    privateKeyContent = privateKeys.get(0);
                } else {
                    LOGGER.log(Level.WARNING, "Maven Server ID {0}: more than one ({1}) private key defined in {1}, use first private key", new Object[]{mavenServerId, privateKeys.size(), sshUserPrivateKey.getId()});
                    privateKeyContent = privateKeys.get(0);
                }

                final Element server = doc.createElement("server");

                // create and add the relevant xml elements
                final Element id = doc.createElement("id");
                id.setTextContent(mavenServerId);

                final Element username = doc.createElement("username");
                username.setTextContent(sshUserPrivateKey.getUsername());

                workDir.mkdirs();
                FilePath privateKeyFile = workDir.createTextTempFile("private-key-", ".pem", privateKeyContent, true);
                privateKeyFile.chmod(0600);
                tempFiles.add(privateKeyFile.getRemote());
                LOGGER.log(Level.FINE, "Create {0}", new Object[]{privateKeyFile.getRemote()});

                final Element privateKey = doc.createElement("privateKey");
                privateKey.setTextContent(privateKeyFile.getRemote());

                final Element passphrase = doc.createElement("passphrase");
                passphrase.setTextContent(Secret.toString(sshUserPrivateKey.getPassphrase()));

                server.appendChild(id);
                server.appendChild(username);
                server.appendChild(privateKey);
                server.appendChild(passphrase);
                copyServerAttributes(currentDefinition,	server);

                serversNode.appendChild(server);
            } else {
                LOGGER.log(Level.WARNING, "Maven Server ID {0}: credentials type of {1} not supported: {2}",
                        new Object[]{mavenServerId, credential == null ? null : credential.getId(), credential == null ? null : credential.getClass()});
            }

        }

        // save the result
        StringWriter writer = new StringWriter();
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.setOutputProperty(OutputKeys.INDENT, "yes");
        xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        xformer.transform(new DOMSource(doc), new StreamResult(writer));
        content = writer.toString();

        return content;
    }

    /*
     * Copy non credential attributes from a node to other
     */
    private static void copyServerAttributes(Node from, Node to) {
        if (from == null || to == null) {
            // nothing to copy	
            return;
        }

        NodeList nodes = from.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = StringUtils.trimToNull(node.getNodeName());
            if (ATTRIBUTES_TO_KEEP.contains(name)) {
                to.appendChild(node);
            }
        }
    }

    /**
     * Removes all children
     * 
     * @param serversNode
     *            the node to remove all children from
     * @param credentialKeys
     *            list of server id to replace
     * @param replaceAll
     *            if remove all server nodes
     */
    private static Map<String, Node> removeMavenServerDefinitions(final Node serversNode, final Set<String> credentialKeys, final boolean replaceAll) {
        Map<String, Node> serverId2Node = new LinkedHashMap<>(credentialKeys.size());

        final NodeList serverNodes = serversNode.getChildNodes();
        for (int i = 0; i < serverNodes.getLength(); i++) {
            final Node server = serverNodes.item(i);
            String serverId = getServerId(server);
            if (replaceAll || (credentialKeys.contains(serverId))) {
                Node removed = serversNode.removeChild(server);
                if (credentialKeys.contains(serverId)) {
                    serverId2Node.put(serverId, removed);
                }
                --i;
            }
        }
        
        
        return serverId2Node;
    }

    private static String getServerId(final Node server) {
        NodeList nodes = server.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = StringUtils.lowerCase(node.getNodeName());
            String content = StringUtils.trimToNull(node.getTextContent());
            if ("id".equals(name)) {
                return content;
            }
        }
        return null;
    }

}
