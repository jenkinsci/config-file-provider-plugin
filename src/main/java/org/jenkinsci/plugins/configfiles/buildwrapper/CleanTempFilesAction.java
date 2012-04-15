package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.FilePath;
import hudson.model.InvisibleAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporal action to transport information about t files to be deleted to the CleanTempFilesRunListener.
 * 
 * @author Dominik Bartholdi (imod)
 * @see org.jenkinsci.plugins.configfiles.buildwrapper.CleanTempFilesRunListener
 */
public class CleanTempFilesAction extends InvisibleAction {

    /**
     * list of the temp files to be removed - never <code>null</code>
     */
    public List<FilePath> tempFiles;

    public CleanTempFilesAction(List<FilePath> tempFiles) {
        this.tempFiles = tempFiles == null ? new ArrayList<FilePath>() : tempFiles;
    }

}