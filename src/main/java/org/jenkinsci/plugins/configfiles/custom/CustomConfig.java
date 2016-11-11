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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.json.JsonConfig;
import org.kohsuke.stapler.DataBoundConstructor;

public class CustomConfig extends Config {
    private static final long serialVersionUID = 1L;

    public CustomConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @DataBoundConstructor
    public CustomConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
    }

    public CustomConfig(Config config){
        super(config);
    }

    @Override
    public ConfigProvider getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(CustomConfigProvider.class);
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
        public <T extends Config> T convert(Config config) {
            return (T) new CustomConfig(config);
        }

        @Override
        public CustomConfig newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new CustomConfig(id, "MyCustom", "", "");
        }

        @NonNull
        @Override
        public CustomConfig newConfig(@NonNull String id) {
            return new CustomConfig(id, "MyCustom", "", "", getProviderId());
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
