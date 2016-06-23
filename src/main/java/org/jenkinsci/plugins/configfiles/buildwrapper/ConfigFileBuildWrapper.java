/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

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

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.tasks.SimpleBuildWrapper;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.kohsuke.stapler.DataBoundConstructor;

public class ConfigFileBuildWrapper extends SimpleBuildWrapper {

    private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

    @DataBoundConstructor
    public ConfigFileBuildWrapper(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }

    @Override public void setUp(Context context, Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        List<String> tempFiles = new ArrayList<String>();
        final Map<ManagedFile, FilePath> file2Path = ManagedFileUtil.provisionConfigFiles(managedFiles, build, workspace, listener, tempFiles);
        for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
            ManagedFile mf = entry.getKey();
            FilePath fp = entry.getValue();
            if (!StringUtils.isBlank(mf.variable)) {
                context.env(mf.variable, fp.getRemote());
            }
            boolean noTargetGiven = StringUtils.isBlank(entry.getKey().targetLocation);
            if (noTargetGiven) {
                tempFiles.add(entry.getValue().getRemote());
            }
        }
        if (!tempFiles.isEmpty()) {
            context.setDisposer(new TempFileCleaner(tempFiles));
        }
    }

    public List<ManagedFile> getManagedFiles() {
        return managedFiles;
    }

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

        public Collection<Config> getConfigFiles() {
            ExtensionList<ConfigProvider> providers = ConfigProvider.all();
            List<Config> allFiles = new ArrayList<Config>();
            for (ConfigProvider provider : providers) {
                allFiles.addAll(provider.getAllConfigs());
            }
            return allFiles;
        }

    }

    private static class TempFileCleaner extends Disposer {
        private final static Logger LOGGER = Logger.getLogger(TempFileCleaner.class.getName());

        private static final long serialVersionUID = 1;

        private final List<String> tempFiles;

        TempFileCleaner(List<String> tempFiles) {
            this.tempFiles = tempFiles;
        }

        @Override public void tearDown(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            listener.getLogger().println("Deleting " + tempFiles.size() + " temporary files");
            for (String tempFile : tempFiles) {
                LOGGER.log(Level.FINE, "Delete: {0}", new Object[]{tempFile});
                new FilePath(workspace, tempFile).delete();
            }
        }

    }

}
