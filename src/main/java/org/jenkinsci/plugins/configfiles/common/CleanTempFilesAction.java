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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;

/**
 * Temporal action to transport information about t files to be deleted to the {@link CleanTempFilesRunListener}.
 * 
 * @author Dominik Bartholdi (imod)
 * @see org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener
 */
public class CleanTempFilesAction extends InvisibleAction implements EnvironmentContributingAction {

    private transient List<String> explicitTempFiles = new ArrayList<String>();

    private transient Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();

    public CleanTempFilesAction(Map<ManagedFile, FilePath> file2Path) {
        this.file2Path = file2Path == null ? Collections.<ManagedFile, FilePath> emptyMap() : file2Path;
        this.explicitTempFiles = Collections.emptyList();
    }

    public CleanTempFilesAction(String tempfile) {
        this.file2Path = Collections.<ManagedFile, FilePath> emptyMap();
        this.explicitTempFiles = new ArrayList<String>();
        this.explicitTempFiles.add(tempfile);
    }

    private Object readResolve() {
        this.file2Path = Collections.<ManagedFile, FilePath> emptyMap();
        this.explicitTempFiles = Collections.emptyList();
        return this;
    }

    /**
     * @Override
     */
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
            ManagedFile mf = entry.getKey();
            FilePath fp = entry.getValue();
            if (!StringUtils.isBlank(mf.variable)) {
                env.put(mf.variable, fp.getRemote());
            }
        }
    }

    /**
     * Provides access to the files which have to be removed after the build
     * 
     * @return a list of paths to the temp files (remotes)
     */
    List<String> getTempFiles() {
        List<String> tempFiles = new ArrayList<String>();
        for (Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
            boolean noTargetGiven = StringUtils.isBlank(entry.getKey().getTargetLocation());
            if (noTargetGiven) {
                tempFiles.add(entry.getValue().getRemote());
            }
        }
        if (explicitTempFiles != null) {
            tempFiles.addAll(explicitTempFiles);
        }
        return tempFiles;
    }

}
