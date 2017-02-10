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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

public class MavenToolchainsConfig extends Config {
    private static final long serialVersionUID = 1L;

    public MavenToolchainsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @DataBoundConstructor
    public MavenToolchainsConfig(String id, String name, String comment, String content, String providerId) {
        super(id, name, comment, content, providerId);
    }

    @Extension(ordinal = 180)
    public static class MavenToolchainsConfigProvider extends AbstractConfigProviderImpl {

        public MavenToolchainsConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.mvn_toolchains_provider_name();
        }

        /* (non-Javadoc)
         * @see org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl#getXmlFileName()
         */
        @Override
        protected String getXmlFileName() {
            return "maven-toolchains-files.xml";
        }

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new MavenToolchainsConfig(id, "MyToolchains", "", loadTemplateContent(), getProviderId());
        }

        @Override
        public <T extends Config> T convert(Config config) {
            return (T) new MavenToolchainsConfig(config.id, config.name, config.comment, config.content, getProviderId());
        }

        private String loadTemplateContent() {
            InputStream in = null;
            try {
                in = this.getClass().getResourceAsStream("toolchains-tpl.xml");
                return IOUtils.toString(in, "UTF-8");
            } catch (Exception e) {
                return "<toolchains></toolchains>";
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

}
