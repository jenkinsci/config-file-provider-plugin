package org.jenkinsci.plugins.configfiles.custom;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

public class CustomConfig extends Config {
    private static final long serialVersionUID = 1L;

    public CustomConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @Extension(ordinal = 50)
    public static class CustomConfigProvider extends AbstractConfigProviderImpl {

        public CustomConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.custom_provider_name();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new Config(id, "MyCustom", "", "");
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("CustomConfigProvider.");
        }

        @Override
        protected String getXmlFileName() {
            return "custom-config-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.custom.CustomConfigProvider", CustomConfigProvider.class);
        }
        // end stuff for backward compatibility
        // ======================

    }

}
