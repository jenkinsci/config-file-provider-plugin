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
package org.jenkinsci.lib.configprovider;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.lib.configprovider.model.ContentType.DefinedType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

public class ExtensionPointTestConfig extends Config {
    public static final String TEST_PARAM_VALUE = "DEFAULT_VALUE";
    public static final String TEST_NAME_VALUE = "ExtensionPointTestConfig";
    public static final String TEST_COMMENT_VALUE = "Test comment";
    public static final String TEST_CONTENT_VALUE = "Test content";

    private static final long serialVersionUID = 1L;

    public String newParam1;
    public String newParam2;
    public String newParam3;

    @DataBoundConstructor
    public ExtensionPointTestConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
        newParam1 = TEST_PARAM_VALUE;
        newParam2 = TEST_PARAM_VALUE;
        newParam3 = TEST_PARAM_VALUE;
    }

    public ExtensionPointTestConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
        newParam1 = TEST_PARAM_VALUE;
        newParam2 = TEST_PARAM_VALUE;
        newParam3 = TEST_PARAM_VALUE;
    }

    public ExtensionPointTestConfig(String id, String name, String comment, String content, String providerId, String newParam1, String newParam2, String newParam3) {
        super(id, name, comment, content, providerId);
        this.newParam1 = newParam1;
        this.newParam2 = newParam2;
        this.newParam3 = newParam3;
    }

    @Extension(ordinal = 500)
    public static class ExtensionPointTestConfigProvider extends AbstractConfigProviderImpl {

        public ExtensionPointTestConfigProvider() {
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

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new ExtensionPointTestConfig(id, TEST_NAME_VALUE, TEST_COMMENT_VALUE, TEST_CONTENT_VALUE, getProviderId(), TEST_PARAM_VALUE, TEST_PARAM_VALUE, TEST_PARAM_VALUE);
        }

        // ======================
        // stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        protected String getXmlFileName() {
            return "extension-point-test-config-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.lib.configprovider.ExtensionPointTestConfigProvider", ExtensionPointTestConfigProvider.class);
        }
        // ======================
    }

}
