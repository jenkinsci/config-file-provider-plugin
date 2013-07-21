package org.jenkinsci.plugins.configfiles.maven.security;

import hudson.model.ItemGroup;
import hudson.util.Secret;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.CredentialsProvider;

public class CredentialsHelper {

    /**
     * hide constructor
     */
    private CredentialsHelper() {
    }

    /**
     * Gets all the maven server credentials storred for the given context
     * 
     * @param scope
     * @return
     */
    public static Map<String, BaseMvnServerCredentials> getCredentials(ItemGroup<?> scope) {
        scope = scope == null ? Jenkins.getInstance() : scope;
        List<BaseMvnServerCredentials> all = CredentialsProvider.lookupCredentials(BaseMvnServerCredentials.class, scope);
        Map<String, BaseMvnServerCredentials> creds = new HashMap<String, BaseMvnServerCredentials>();
        for (BaseMvnServerCredentials u : all) {
            creds.put(u.getId(), u);
        }
        return creds;
    }

    public static String fillAuthentication(String settingsContent, Map<String, BaseMvnServerCredentials> credentials) throws Exception {
        String content = settingsContent;
        if (!credentials.isEmpty()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(content)));

            // locate the node(s)
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);

            for (int idx = 0; idx < nodes.getLength(); idx++) {
                final Node server = nodes.item(idx);
                final String serverId = xpath.evaluate("id", server);
                if (StringUtils.isNotBlank(serverId)) {
                    final BaseMvnServerCredentials serverCredentials = credentials.get(serverId);
                    if (serverCredentials != null && serverCredentials instanceof MvnServerPassword) {

                        // at this stage, we have both:
                        // - we know there is a serverconfig in the settings.xml
                        // - we have the configured credentials
                        // so we know how to configure it, therefore remove all the old content from the settings.xml
                        // this will allow an easy switch between username/password and privateKey/passphrase by the user (no matter what's already in the settigns.xml)
                        removeAllChilds(server);

                        // add the relevant xml elements
                        final Element id = doc.createElement("id");
                        id.setTextContent(serverId);
                        MvnServerPassword pwd = (MvnServerPassword) serverCredentials;
                        final Element password = doc.createElement("password");
                        password.setTextContent(Secret.toString(pwd.getPassword()));
                        final Element username = doc.createElement("username");
                        username.setTextContent(pwd.getUsername());

                        server.appendChild(id);
                        server.appendChild(username);
                        server.appendChild(password);
                    }
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
