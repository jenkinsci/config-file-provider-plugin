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
package org.jenkinsci.lib.configprovider.model;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.Messages;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import hudson.slaves.WorkspaceList;

public class ConfigFileManager {
    private final static Logger LOGGER = Logger.getLogger(ConfigFileManager.class.getName());

    /**
     * TODO use 1.652 use WorkspaceList.tempDir
     *
     * @param ws workspace of the {@link hudson.model.Build}. See {@link Build#getWorkspace()}
     * @return temporary directory, may not have been created so far
     */
    public static FilePath tempDir(FilePath ws) {
        return ws.sibling(ws.getName() + System.getProperty(WorkspaceList.class.getName(), "@") + "tmp");
    }

    /**
     * Provisions (publishes) the given file to the workspace.
     *
     * @param managedFile  the file to be provisioned
     * @param workspace    target workspace
     * @param listener     the listener
     * @param tempFiles    temp files created by this method, these files should be deleted by the caller
     * @return remote location path of the provided file.
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException       config file has not been found
     */
    public static FilePath provisionConfigFile(ConfigFile managedFile, Run<?, ?> build, FilePath workspace, TaskListener listener, List<String> tempFiles) throws IOException, InterruptedException {
        Config configFile = ConfigFiles.getByIdOrNull(build, managedFile.getFileId());

        if (configFile == null) {
            String message = "not able to provide the file " + managedFile + ", can't be resolved by any provider - maybe it got deleted by an administrator?";
            listener.getLogger().println(message);
            throw new AbortException(message);
        }

        FilePath workDir = tempDir(workspace);
        workDir.mkdirs();

        boolean createTempFile = StringUtils.isBlank(managedFile.getTargetLocation());

        FilePath target;
        if (createTempFile) {
            target = workDir.createTempFile("config", "tmp");
        } else {

            String expandedTargetLocation = managedFile.getTargetLocation();
            try {
                expandedTargetLocation = TokenMacro.expandAll(build, workspace, listener, managedFile.getTargetLocation());
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in target location '" + managedFile.getTargetLocation() + "' : " + e.getMessage());
                expandedTargetLocation = managedFile.getTargetLocation();
            }

            // Should treat given path as the actual filename unless it has a trailing slash (implying a
            // directory) or path already exists in workspace as a directory.
            target = new FilePath(workspace, expandedTargetLocation);
            String immediateFileName = expandedTargetLocation.substring(expandedTargetLocation.lastIndexOf("/") + 1);

            if (immediateFileName.length() == 0 || (target.exists() && target.isDirectory())) {
                target = new FilePath(target, configFile.name.replace(" ", "_"));
            }
        }

        ConfigProvider provider = configFile.getDescriptor();
        String fileContent = provider.supplyContent(configFile, build, workDir, listener, tempFiles);

        if (managedFile.isReplaceTokens()) {
            try {
                fileContent = TokenMacro.expandAll(build, workspace, listener, fileContent);
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in content of " + configFile.name + " - " + e.getMessage());
            }
        }

        LOGGER.log(Level.FINE, "Create file {0} for configuration {1} mapped as {2}", new Object[]{target.getRemote(), configFile, managedFile});
        listener.getLogger().println(Messages.console_output(configFile.name, target.toURI()));
        ByteArrayInputStream bs = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
        target.copyFrom(bs);
        target.chmod(0640);

        return target;
    }

}