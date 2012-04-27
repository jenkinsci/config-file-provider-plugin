package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.model.InvisibleAction;

import java.util.Collections;
import java.util.List;

/**
 * Temporal action to transport information about t files to be deleted to the CleanTempFilesRunListener.
 * 
 * @author Dominik Bartholdi (imod)
 * @see org.jenkinsci.plugins.configfiles.buildwrapper.CleanTempFilesRunListener
 */
public class CleanTempFilesAction extends InvisibleAction {

    private final transient List<String> tempFiles;

    public CleanTempFilesAction(List<String> tempFiles) {
        this.tempFiles = tempFiles;
    }

    /**
     * List of the temp files to be removed - never <code>null</code>.
     * 
     * @return list of temp files
     */
    public List<String> getTempFiles() {
        return tempFiles == null ? Collections.<String> emptyList() : tempFiles;
    }

}