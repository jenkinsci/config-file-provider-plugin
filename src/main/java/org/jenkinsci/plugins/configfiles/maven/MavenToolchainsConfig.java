/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi, Olivier Lamy

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles.maven;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MavenToolchainsConfig extends Config {
    private static final long serialVersionUID = 1L;

    public Boolean removeUnavailableJdkToolchains = removeUnavailableJdkToolchainsDefault;
    public static final Boolean removeUnavailableJdkToolchainsDefault = Boolean.FALSE;

    @DataBoundConstructor
    public MavenToolchainsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    public MavenToolchainsConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
    }

    public Boolean getRemoveUnavailableJdkToolchains() {
        return (removeUnavailableJdkToolchains != null) ? removeUnavailableJdkToolchains : removeUnavailableJdkToolchainsDefault;
    }

    @DataBoundSetter
    public void setRemoveUnavailableJdkToolchains(Boolean removeUnavailableJdkToolchains) {
        this.removeUnavailableJdkToolchains = removeUnavailableJdkToolchains != null ? removeUnavailableJdkToolchains : removeUnavailableJdkToolchainsDefault;
    }

    @Extension(ordinal = 180)
    public static class MavenToolchainsConfigProvider extends AbstractConfigProviderImpl {

        public MavenToolchainsConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.mvn_toolchains_provider_name();
        }

        /* (non-Javadoc)
         * @see org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl#getXmlFileName()
         */
        @Override
        protected String getXmlFileName() {
            return "maven-toolchains-files.xml";
        }


        @CheckForNull
        @Override
        public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
            String fileContent = super.supplyContent(configFile, build, workDir, listener, tempFiles);
            if (fileContent == null) {
                return null;
            }

            List<JDK> jdks = Jenkins.get().getJDKs();
            if (jdks.isEmpty()) {
                return fileContent;
            }

            return replaceJDKHomesIn(fileContent, jdks, (MavenToolchainsConfig) configFile, build, workDir, listener);
        }

        private String replaceJDKHomesIn(String fileContent, List<JDK> jdks, MavenToolchainsConfig configFile, Run<?, ?> build, FilePath workDir, TaskListener listener) throws IOException {
            List<String> unmatchedJDKs = new ArrayList<>(jdks.size());
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(fileContent)));
                XPath xpath = XPathFactory.newInstance().newXPath();

                for (JDK jdk : jdks) {
                    String jdkName = jdk.getName();
                    String selector = String.format("/toolchains/toolchain[./type[text()='jdk']/../provides/id[text()='%1$s']]", jdkName);
                    Element existingToolChain = (Element) xpath.evaluate(selector, doc, XPathConstants.NODE);
                    if (existingToolChain != null) {
                        Element configurationElement = getOrCreateChildElement(doc, existingToolChain, "configuration");
                        Element jdkHomeElement = getOrCreateChildElement(doc, configurationElement, "jdkHome");
                        jdkHomeElement.setTextContent(jdk.getHome());
                    } else {
                        unmatchedJDKs.add(jdkName);
                    }
                }
                if (unmatchedJDKs.size() > 0) {
                    StringBuilder msg = new StringBuilder("WARNING: unable to set jdkHome in generated toolchains.xml file because no toolchains with id ");
                    for (int i = 0; i < unmatchedJDKs.size(); i++) {
                        msg.append("'" + unmatchedJDKs.get(i) + "'");
                        if (i + 1 < unmatchedJDKs.size()) {
                             msg.append(", ");
                        }
                    }
                    msg.append(" are available");
                    listener.getLogger().println(msg.toString());
                }

                if (configFile.getRemoveUnavailableJdkToolchains()) {
                    removeUnavailableJdkToolchains(doc, configFile, build, workDir, listener);
                }

                // save the result
                StringWriter writer = new StringWriter();
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
                xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                xformer.transform(new DOMSource(doc), new StreamResult(writer));
                return writer.toString();
            } catch (SAXException | ParserConfigurationException | TransformerException | XPathExpressionException | DOMException | InterruptedException exc) {
                throw new IOException("Unable to generate toolchains.xml " + configFile, exc);
            }
        }

        private void removeUnavailableJdkToolchains(Document doc, MavenToolchainsConfig configFile, Run<?, ?> build, FilePath workDir, TaskListener listener) throws XPathExpressionException, DOMException, IOException, InterruptedException {
            Node buildNode = null;
            Computer computer = workDir.toComputer();
            if (computer != null) {
                buildNode = computer.getNode();
            }
            if (buildNode == null) {
                buildNode = Jenkins.get();
            }

            XPath xpath = XPathFactory.newInstance().newXPath();
            String selector = "/toolchains/toolchain[./type[text()='jdk']/../configuration/jdkHome]";
            NodeList existingToolchains = (NodeList) xpath.evaluate(selector, doc, XPathConstants.NODESET);
            if (existingToolchains != null && existingToolchains.getLength() > 0) {
                Element parent = (Element) existingToolchains.item(0).getParentNode();
                for (int i = existingToolchains.getLength() - 1; i >= 0; i--) {
                    Element toolchain = (Element) existingToolchains.item(i);
                    NodeList jdkHomes = toolchain.getElementsByTagName("jdkHome");
                    if (jdkHomes != null) {
                        String jdkHome = jdkHomes.item(0).getTextContent();
                        if (jdkHome != null) {
                            FilePath jdkPath = buildNode.createPath(jdkHome);
                            if (jdkPath != null && !jdkPath.exists()) {
                                listener.getLogger().println(String.format("Removing toolchain from provided toolchains, since jdk does not exist on %1$s: %2$s", buildNode.getDisplayName(), jdkHome));
                                parent.removeChild(toolchain);
                            } else {
                                listener.getLogger().println(String.format("jdk exists on %1$s: %2$s", buildNode.getDisplayName(), jdkHome));
                            }
                        }
                    }
                }
            }
        }

        private Element getOrCreateChildElement(Document doc, Element parent, String elementName) {
            NodeList configurationList = parent.getElementsByTagName(elementName);
            if (configurationList.getLength() == 0) {
                Element element = doc.createElement(elementName);
                parent.appendChild(element);
                return element;
            } else {
                return (Element) configurationList.item(0);
            }
        }

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new MavenToolchainsConfig(id,
                Messages.MavenToolchains_SettingsName(),
                Messages.MavenToolchains_SettingsComment(),
                loadTemplateContent(),
                getProviderId());
        }

        private String loadTemplateContent() {
            InputStream in = null;
            try {
                in = this.getClass().getResourceAsStream("toolchains-tpl.xml");
                return IOUtils.toString(in, "UTF-8");
            } catch (Exception e) {
                return "<toolchains></toolchains>";
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
}
