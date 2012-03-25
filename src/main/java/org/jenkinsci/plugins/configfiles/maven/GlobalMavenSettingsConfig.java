package org.jenkinsci.plugins.configfiles.maven;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

public class GlobalMavenSettingsConfig extends Config {
    private static final long serialVersionUID = 1L;

    public GlobalMavenSettingsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @Extension(ordinal = 200)
    public static class GlobalMavenSettingsConfigProvider extends AbstractMavenSettingsProvider {

        public GlobalMavenSettingsConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.mvn_global_settings_provider_name();
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("DefaultGlobalMavenSettingsProvider.");
        }

        @Override
        protected String getXmlFileName() {
            return "maven-global-settings-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.maven.DefaultGlobalMavenSettingsProvider", GlobalMavenSettingsConfigProvider.class);
        }
        // end stuff for backward compatibility
        // ======================

    }

}
