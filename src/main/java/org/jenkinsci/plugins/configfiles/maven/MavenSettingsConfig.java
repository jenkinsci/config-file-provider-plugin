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

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

public class MavenSettingsConfig extends Config {
    private static final long serialVersionUID = 1L;

    public MavenSettingsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @Extension(ordinal = 190)
    public static class MavenSettingsConfigProvider extends AbstractMavenSettingsProvider {

        public MavenSettingsConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.mvn_settings_provider_name();
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("DefaultMavenSettingsProvider.");
        }

        @Override
        protected String getXmlFileName() {
            return "maven-settings-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.maven.DefaultMavenSettingsProvider", MavenSettingsConfigProvider.class);
        }
        // end stuff for backward compatibility
        // ======================

    }

}
