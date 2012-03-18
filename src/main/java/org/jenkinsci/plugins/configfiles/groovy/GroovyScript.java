package org.jenkinsci.plugins.configfiles.groovy;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.lib.configprovider.model.ContentType.DefinedType;
import org.jenkinsci.plugins.configfiles.Messages;

public class GroovyScript extends Config {
    private static final long serialVersionUID = 1L;

    public GroovyScript(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @Extension(ordinal = 100)
    public static class GroovyConfigProvider extends AbstractConfigProviderImpl {

        public GroovyConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return DefinedType.GROOVY;
        }

        @Override
        public String getDisplayName() {
            return Messages.groovy_provider_name();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new Config(id, "GroovyConfig", "", "println('hello world')");
        }

        // ======================
        // stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("GroovyConfigProvider.");
        }

        @Override
        protected String getXmlFileName() {
            return "groovy-config-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.groovy.GroovyConfigProvider", GroovyConfigProvider.class);
        }
        // ======================
    }

}
