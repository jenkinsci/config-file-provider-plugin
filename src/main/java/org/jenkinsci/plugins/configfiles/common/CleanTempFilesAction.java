package org.jenkinsci.plugins.configfiles.common;
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
