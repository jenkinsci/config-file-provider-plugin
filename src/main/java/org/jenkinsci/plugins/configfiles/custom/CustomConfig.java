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
package org.jenkinsci.plugins.configfiles.custom;

import hudson.Extension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.custom.security.CustomizedCredentialMapping;
import org.jenkinsci.plugins.configfiles.custom.security.HasCustomizedCredentialMappings;
import org.kohsuke.stapler.DataBoundConstructor;

public class CustomConfig extends Config implements HasCustomizedCredentialMappings {
    private static final long serialVersionUID = 1L;

    private List<CustomizedCredentialMapping> customizedCredentialMappings;

    public CustomConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
        this.customizedCredentialMappings = new ArrayList<CustomizedCredentialMapping>();
    }

    @DataBoundConstructor
    public CustomConfig(String id, String name, String comment, String content, List<CustomizedCredentialMapping> customizedCredentialMappings) {
        super(id, name, comment, content, CustomConfigProvider.class.getName());
        this.customizedCredentialMappings = customizedCredentialMappings == null ? new ArrayList<CustomizedCredentialMapping>() : customizedCredentialMappings;
    }

    public CustomConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
        this.customizedCredentialMappings = new ArrayList<CustomizedCredentialMapping>();
    }

    @Override
    public List<CustomizedCredentialMapping> getCustomizedCredentialMappings() {
        return customizedCredentialMappings == null ? new ArrayList<CustomizedCredentialMapping>() : customizedCredentialMappings;
    }

    @Extension(ordinal = 50)
    public static class CustomConfigProvider extends AbstractCustomProvider {

        public CustomConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.SHELL;
        }

        @Override
        public String getDisplayName() {
            return Messages.custom_provider_name();
        }

        @Nonnull
        @Override
        public CustomConfig newConfig(@Nonnull String id) {
            return new CustomConfig(id,
                Messages.CustomConfig_SettingsName(),
                Messages.CustomConfig_SettingsComment(),
                "",
                getProviderId());
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;


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
