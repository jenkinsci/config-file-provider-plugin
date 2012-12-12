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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.DataBoundConstructor;

public class ConfigFileBuildWrapper extends BuildWrapper {

    private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

    @DataBoundConstructor
    public ConfigFileBuildWrapper(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }

    @Override
    public Environment setUp(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        final PrintStream logger = listener.getLogger();

        if (build.getWorkspace() == null) {
            throw new IllegalStateException("the workspace does not yet exist, can't provision config files - maybe slave is offline?");
        }

        final Map<ManagedFile, FilePath> file2Path = ManagedFileUtil.provisionConfigFiles(managedFiles, build, listener);
        // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
        build.addAction(new CleanTempFilesAction(file2Path));

        return new Environment() {
        };
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

}
