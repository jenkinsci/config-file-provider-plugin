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

import java.io.IOException;
import java.io.InputStream;

import hudson.util.IOUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;

/**
 * @author Olivier Lamy
 * @author Dominik Bartholdi (imod)
 */
public abstract class AbstractMavenSettingsProvider extends AbstractConfigProviderImpl {

    @Override
    public Config newConfig() {
        String id = this.getProviderId() + System.currentTimeMillis();
        return new Config(id, "MySettings", "", loadTemplateContent());
    }

    @Override
    public ContentType getContentType() {
        return ContentType.DefinedType.XML;
    }

    protected String loadTemplateContent() {
        InputStream in = null;
        try {
            in = AbstractMavenSettingsProvider.class.getResourceAsStream("settings-tpl.xml");
            return org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        } catch (Exception e) {
            return "<settings></settings>";
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(in);
        }
    }
}
