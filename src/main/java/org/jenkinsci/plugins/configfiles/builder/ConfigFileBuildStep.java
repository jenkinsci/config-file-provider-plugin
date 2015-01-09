/**
 * 
 */
package org.jenkinsci.plugins.configfiles.builder;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFileUtil;
import org.jenkinsci.plugins.configfiles.buildwrapper.Messages;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Dominik Bartholdi (imod)
 * 
 */
public class ConfigFileBuildStep extends Builder implements SimpleBuildStep {

    private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

    @DataBoundConstructor
    public ConfigFileBuildStep(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }
    
    public List<ManagedFile> getManagedFiles() {
        return managedFiles;
    }    
	
	public void setManagedFiles(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }
	
	@Override
	public void perform(Run<?, ?> build, FilePath ws, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		if (ws == null) {
            throw new IllegalStateException("the workspace does not yet exist, can't provision config files - maybe slave is offline?");
        }

        final Map<ManagedFile, FilePath> file2Path = ManagedFileUtil.provisionConfigFiles(managedFiles, build, ws, listener);
        // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
        build.addAction(new CleanTempFilesAction(file2Path));

        return;
	}

   
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return Messages.display_name();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
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
