package org.jenkinsci.plugins.configfiles.xml;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

public class XmlConfig extends Config {
    private static final long serialVersionUID = 1L;

    public XmlConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
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
            return new Config(id, "XmlConfig", "", "<root></root>");
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("XmlConfigProvider.");
        }

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
