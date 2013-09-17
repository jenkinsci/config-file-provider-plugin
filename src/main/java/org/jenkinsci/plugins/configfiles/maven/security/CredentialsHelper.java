package org.jenkinsci.plugins.configfiles.maven.security;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

public class CredentialsHelper {

    /**
     * hide constructor
     */
    private CredentialsHelper() {
    }

    public static String fillAuthentication(String settingsContent, List<BaseStandardCredentials> credentialsList) throws Exception {
        String content = settingsContent;
        if (!credentialsList.isEmpty()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(content)));

            Map<String, BaseStandardCredentials> id2c = new HashMap<String, BaseStandardCredentials>();
            for (BaseStandardCredentials c : credentialsList) {
                id2c.put(c.getId(), c);
            }

            // locate the server node(s)
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node serversNode = (Node) xpath.evaluate("/settings/servers/", doc, XPathConstants.NODE);
            if (serversNode == null) {
                // need to create a 'servers' node
                Node settingsNode = (Node) xpath.evaluate("/settings/", doc, XPathConstants.NODE);
                serversNode = doc.createElement("servers");
                settingsNode.appendChild(serversNode);
            } else {
                // remove all server nodes, we will replace every single one and only add the ones provided
                removeAllChilds(serversNode);
            }

            for (BaseStandardCredentials credentials : credentialsList) {

                if (credentials instanceof StandardUsernamePasswordCredentials) {

                    StandardUsernamePasswordCredentials userPwd = (StandardUsernamePasswordCredentials) credentials;

                    final Element server = doc.createElement("server");

                    // create and add the relevant xml elements
                    final Element id = doc.createElement("id");
                    id.setTextContent(userPwd.getId());
                    final Element password = doc.createElement("password");
                    password.setTextContent(Secret.toString(userPwd.getPassword()));
                    final Element username = doc.createElement("username");
                    username.setTextContent(userPwd.getUsername());

                    server.appendChild(id);
                    server.appendChild(username);
                    server.appendChild(password);

                    serversNode.appendChild(server);
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
