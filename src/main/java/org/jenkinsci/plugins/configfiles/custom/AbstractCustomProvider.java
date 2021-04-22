/*
 The MIT License

 Copyright (c) 2020, Andrew Grimberg

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

import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.custom.security.CustomConfigCredentialsHelper;
import org.jenkinsci.plugins.configfiles.custom.security.HasCustomizedCredentialMappings;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractCustomProvider extends AbstractConfigProviderImpl {

    @Override
    public ContentType getContentType() {
        return ContentType.DefinedType.SHELL;
    }

    @Override
    public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {

        HasCustomizedCredentialMappings settings = (HasCustomizedCredentialMappings) configFile;

        final Map<String, IdCredentials> resolvedCredentials = CustomConfigCredentialsHelper.resolveCredentials(build, settings.getCustomizedCredentialMappings(), listener);

        String fileContent = super.supplyContent(configFile, build, workDir, listener, tempFiles);
        if (!resolvedCredentials.isEmpty()) {
            try {
                fileContent = CustomConfigCredentialsHelper.fillAuthentication(build, workDir, listener, fileContent, resolvedCredentials);
            } catch (Exception exception) {
                throw new IOException("[ERROR] could not insert credentials into the settings file " + configFile, exception);
            }
        }
        return fileContent;
    }
}
