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
package org.jenkinsci.plugins.configfiles.buildwrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.util.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ManagedFileUtil {
    private final static Logger LOGGER = Logger.getLogger(ManagedFileUtil.class.getName());

    /**
     * provisions (publishes) the given files to the workspace.
     *
     * @param managedFiles the files to be provisioned
     * @param workspace    target workspace
     * @param listener     the listener
     * @param tempFiles    temp files created by this method, these files should be deleted by the caller
     * @return a map of all the files copied, mapped to the path of the remote location, never <code>null</code>.
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException       config file has not been found
     */
    public static Map<ManagedFile, FilePath> provisionConfigFiles(List<ManagedFile> managedFiles, Run<?, ?> build, FilePath workspace, TaskListener listener, List<String> tempFiles) throws IOException, InterruptedException {

        final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();
        listener.getLogger().println("provisioning config files...");

        for (ManagedFile managedFile : managedFiles) {
            FilePath target = ConfigFileManager.provisionConfigFile(managedFile.fileId, managedFile.targetLocation,
                    managedFile.getReplaceTokens(), null, build, workspace, listener, tempFiles);

            Config configFile = ConfigFiles.getByIdOrNull(build, managedFile.fileId);
            if (configFile != null) {
                // null check is guarantee by provisionConfigFile
                LOGGER.log(Level.FINE, "Create file {0} for configuration {1} mapped as {2}", new Object[] { target.getRemote(), configFile, managedFile });
                listener.getLogger().println(Messages.console_output(configFile.name, target.toURI()));
            }
            file2Path.put(managedFile, target);
        }

        return file2Path;
    }

}