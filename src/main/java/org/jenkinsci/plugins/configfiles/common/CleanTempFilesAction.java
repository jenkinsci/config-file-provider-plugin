package org.jenkinsci.plugins.configfiles.common;

import hudson.model.InvisibleAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Temporal action to transport information about t files to be deleted to the CleanTempFilesRunListener.
 * 
 * @author Dominik Bartholdi (imod)
 * @see org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener
 */
public class CleanTempFilesAction extends InvisibleAction {

    private final transient List<String> tempFiles = new ArrayList<String>();

    public CleanTempFilesAction(List<String> tempFiles) {
        this.tempFiles.addAll(tempFiles);
    }

    public CleanTempFilesAction(String tempFile) {
        this.tempFiles.add(tempFile);
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