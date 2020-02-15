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

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.maven.security.HasCredentialMappings;
import org.jenkinsci.plugins.configfiles.maven.security.proxy.ProxyCredentialsHelper;
import org.jenkinsci.plugins.configfiles.maven.security.server.ServerCredentialsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @author Dominik Bartholdi (imod)
 */
public abstract class AbstractMavenSettingsProvider extends AbstractConfigProviderImpl {

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

    @Override
    public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
        HasCredentialMappings settings = (HasCredentialMappings) configFile;
        final Map<String, StandardUsernameCredentials> resolvedServerCredentials = ServerCredentialsHelper.resolveCredentials(build, settings.getServerCredentialMappings(), listener);
        final Boolean isReplaceAll = settings.getIsReplaceAll();

        String fileContent = super.supplyContent(configFile, build, workDir, listener, tempFiles);
        if (!resolvedServerCredentials.isEmpty()) {
            try {
                fileContent = ServerCredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedServerCredentials, workDir, tempFiles);
            } catch (Exception exception) {
                throw new IOException("[ERROR] could not insert server credentials into the settings file " + configFile, exception);
            }
        }

        final Map<String, StandardUsernameCredentials> resolvedProxyCredentials = ProxyCredentialsHelper.resolveCredentials(build, settings.getProxyCredentialMappings(), listener);

        if (!resolvedProxyCredentials.isEmpty()) {
            try {
                fileContent = ProxyCredentialsHelper.fillAuthentication(fileContent, resolvedProxyCredentials);
            } catch (Exception exception) {
                throw new IOException("[ERROR] could not insert proxy credentials into the settings file " + configFile, exception);
            }
        }

        return fileContent;
    }
}
