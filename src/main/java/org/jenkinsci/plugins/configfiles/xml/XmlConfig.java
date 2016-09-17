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
package org.jenkinsci.plugins.configfiles.xml;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.kohsuke.stapler.DataBoundConstructor;

public class XmlConfig extends Config {
    private static final long serialVersionUID = 1L;

    public XmlConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @DataBoundConstructor
    public XmlConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
    }

    public XmlConfig(Config config){
        super(config);
    }

    @Override
    public ConfigProvider getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(XmlConfigProvider.class);
    }

    @Extension(ordinal = 150)
    public static class XmlConfigProvider extends AbstractConfigProviderImpl {

        public XmlConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.xml_provider_name();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new XmlConfig(id, "XmlConfig", "", "<root></root>");
        }

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new XmlConfig(id, "XmlConfig", "", "<root></root>", getProviderId());
        }

        @Override
        public <T extends Config> T convert(Config config) {
            return (T) new XmlConfig(config);
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        protected String getXmlFileName() {
            return "xml-config-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.xml.XmlConfigProvider", XmlConfigProvider.class);
        }
        // end stuff for backward compatibility
        // ======================

    }

}
