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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.maven.security.HasServerCredentialMappings;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.kohsuke.stapler.DataBoundConstructor;

public class GlobalMavenSettingsConfig extends Config implements HasServerCredentialMappings {
    private static final long serialVersionUID = 1L;

    private List<ServerCredentialMapping> serverCredentialMappings;

    /**
     * Should server elements not contained in the credentials list be merged, or cleaned.
     * Cleaned is the traditional behaviour and means the server entry won't appear in the resulting xml file.
     */
    public Boolean isReplaceAll = isReplaceAllDefault;
    public static final Boolean isReplaceAllDefault = Boolean.TRUE;

    @DataBoundConstructor
    public GlobalMavenSettingsConfig(String id, String name, String comment, String content, Boolean isReplaceAll, List<ServerCredentialMapping> serverCredentialMappings) {
        super(id, name, comment, content);
        this.serverCredentialMappings = serverCredentialMappings == null ? new ArrayList<ServerCredentialMapping>() : serverCredentialMappings;
        this.isReplaceAll = (null == isReplaceAll) ? isReplaceAllDefault : isReplaceAll;
    }
    
    public GlobalMavenSettingsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }
    
    public List<ServerCredentialMapping> getServerCredentialMappings() {
        return serverCredentialMappings;
    }

    public Boolean getIsReplaceAll() {
        return isReplaceAll;
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
        
        @Override
        public Config newConfig(String idSuffix) {
            String id =  this.getProviderId() + "." + StringUtils.defaultIfBlank(idSuffix, String.valueOf(System.currentTimeMillis()));
            return new GlobalMavenSettingsConfig(id, "MyGlobalSettings", "global settings", loadTemplateContent(), GlobalMavenSettingsConfig.isReplaceAllDefault, Collections.<ServerCredentialMapping>emptyList());
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
