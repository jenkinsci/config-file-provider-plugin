package org.jenkinsci.plugins.configfiles.builder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFileUtil;
import org.jenkinsci.plugins.configfiles.buildwrapper.Messages;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Dominik Bartholdi (imod)
 * 
 */
public class ConfigFileBuildStep extends Builder implements Serializable {

    private static final long serialVersionUID = -5623878268985950032L;


    private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

    @DataBoundConstructor
    public ConfigFileBuildStep(List<ManagedFile> managedFiles) {
        this.managedFiles = managedFiles;
    }
    
    public List<ManagedFile> getManagedFiles() {
        return managedFiles;
    }    

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        if (build.getWorkspace() == null) {
            throw new IllegalStateException("the workspace does not yet exist, can't provision config files - maybe the agent is offline?");
        }

        List<String> tempFiles = new ArrayList<String>();

        final Map<ManagedFile, FilePath> file2Path = ManagedFileUtil.provisionConfigFiles(managedFiles, null, build, build.getWorkspace(), listener, tempFiles);

        for (String tempFile:tempFiles) {
            build.addAction(new CleanTempFilesAction(tempFile));
        }
        // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
        build.addAction(new CleanTempFilesAction(file2Path));

        return true;
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

    }

}
