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

import java.io.InputStream;
import java.io.InputStreamReader;

import hudson.Extension;
import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

public class MavenToolchainsConfig extends Config {
    private static final long serialVersionUID = 1L;

    public MavenToolchainsConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
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

        /* (non-Javadoc)
         * @see org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl#newConfig()
         */
        @Override
        public Config newConfig() {
            String id = this.getProviderId() + System.currentTimeMillis();
            return new Config(id, "MyToolchains", "", loadTemplateContent());
         }

        private String loadTemplateContent() {
            String tpl;
            try {
                InputStream is = this.getClass().getResourceAsStream("toolchains-tpl.xml");
                StringBuilder sb = new StringBuilder(Math.max(16, is.available()));
                char[] tmp = new char[4096];

                try {
                    InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                    for (int cnt; (cnt = reader.read(tmp)) > 0;)
                        sb.append(tmp, 0, cnt);

                } finally {
                    is.close();
                }
                tpl = sb.toString();
            } catch (Exception e) {
                tpl = "<toolchains></toolchains>";
            }
            return tpl;
        }

    }

}
