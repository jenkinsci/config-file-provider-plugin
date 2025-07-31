/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi
 Copyright (c) 2023, CloudBees Inc.

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
package org.jenkinsci.plugins.configfiles.buildwrapper;

import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.Secret;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jenkins.tasks.SimpleBuildWrapper;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.credentialsbinding.masking.SecretPatterns;

public class ConfigFileBuildWrapper extends SimpleBuildWrapper {

    private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

    @DataBoundConstructor
    public ConfigFileBuildWrapper(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        List<String> tempFiles = new ArrayList<String>();

        final Map<ManagedFile, FilePath> file2Path = ManagedFileUtil.provisionConfigFiles(managedFiles, initialEnvironment, build, workspace, listener, tempFiles);
        for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
            ManagedFile mf = entry.getKey();
            FilePath fp = entry.getValue();
            if (!(mf.variable == null || mf.variable.isBlank())) {
                context.env(mf.variable, fp.getRemote());
            }
            final String targetLocation = entry.getKey().getTargetLocation();
            boolean noTargetGiven = targetLocation == null || targetLocation.isBlank();
            if (noTargetGiven) {
                tempFiles.add(entry.getValue().getRemote());
            }
        }
        if (!tempFiles.isEmpty()) {
            context.setDisposer(new TempFileCleaner(tempFiles));
        }
    }

    private synchronized List<String> getSecretValuesToMask(Run<?,?> build) {
        List<String> seecretsToMask = new ArrayList<>();
        for (ManagedFile managedFile : managedFiles) {
            Config config = ConfigFiles.getByIdOrNull(build, managedFile.getFileId());
            seecretsToMask.addAll(config.getProvider().getSensitiveContentForMasking(config, build));
        }
        return seecretsToMask;
    }

    @Override
    public ConsoleLogFilter createLoggerDecorator(@NonNull Run<?, ?> build) {
        List<String> secretValues = getSecretValuesToMask(build);
        if (secretValues.isEmpty()) {
            // no secrets so no filtering
            return null;
        }
        return new SecretFilter(secretValues, build.getCharset());
    }

    public List<ManagedFile> getManagedFiles() {
        return managedFiles;
    }

    @Symbol("configFileProvider")
    @Extension(ordinal = 50)
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.display_name();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }

    private static class TempFileCleaner extends Disposer {
        private final static Logger LOGGER = Logger.getLogger(TempFileCleaner.class.getName());

        private static final long serialVersionUID = 1;

        private final List<String> tempFiles;

        TempFileCleaner(List<String> tempFiles) {
            this.tempFiles = tempFiles;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            listener.getLogger().println("Deleting " + tempFiles.size() + " temporary files");
            for (String tempFile : tempFiles) {
                LOGGER.log(Level.FINE, "Delete: {0}", new Object[]{tempFile});
                new FilePath(workspace, tempFile).delete();
            }
        }

    }

    private static final class SecretFilter extends ConsoleLogFilter implements Serializable {

        private static final long serialVersionUID = 1;

        private Secret pattern;
        private String charset;

        SecretFilter(Collection<String> secrets, Charset cs) {
            pattern = Secret.fromString(SecretPatterns.getAggregateSecretPattern(secrets).pattern());
            charset = cs.name();
        }

        @Override
        public OutputStream decorateLogger(Run build, OutputStream logger) {
            return new SecretPatterns.MaskingOutputStream(logger, () -> Pattern.compile(pattern.getPlainText()), charset);
        }

    }

}
