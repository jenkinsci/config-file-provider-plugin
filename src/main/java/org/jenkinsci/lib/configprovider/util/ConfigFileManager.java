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
package org.jenkinsci.lib.configprovider.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Build;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class ConfigFileManager {
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
     * Provisions (publishes) the given files to the workspace.
     *
     * @param fileId config identifier to be provisioned
     * @param targetLocation a custom target location where provide the config file or {@literal null} to provide file in a default secure location
     * @param replaceTokens if {@literal true} performs the variable substitution of the config file content
     * @param env enhanced environment to use in the variable substitution
     * @param build a build being run
     * @param workspace a workspace of the build
     * @param listener a way to report progress
     * @param tempFiles temp files created in this method, these files should be deleted by the caller
     * @return path of the remote location of the config
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException in case config file has not been found
     */
    public static FilePath provisionConfigFile(@NonNull String fileId, @Nullable String targetLocation, boolean replaceTokens, @Nullable EnvVars env,
            Run<?, ?> build, FilePath workspace, TaskListener listener, @NonNull List<String> tempFiles) throws IOException, InterruptedException {
        Config configFile = ConfigFiles.getByIdOrNull(build, fileId);

        if (configFile == null) {
            String message = "not able to provide the file " + fileId + ", can't be resolved by any provider - maybe it got deleted by an administrator?";
            listener.getLogger().println(message);
            throw new AbortException(message);
        }

        FilePath workDir = tempDir(workspace);
        workDir.mkdirs();

        boolean createTempFile = StringUtils.isBlank(targetLocation);

        FilePath target;
        if (createTempFile) {
            target = workDir.createTempFile("config", "tmp");
        } else {

            String expandedTargetLocation = targetLocation;
            try {
                expandedTargetLocation = TokenMacro.expandAll(build, workspace, listener, targetLocation);
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in target location '" + targetLocation + "' : " + e.getMessage());
                expandedTargetLocation = targetLocation;
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

        if (replaceTokens) {
            try {
                fileContent = TokenMacro.expandAll(build, workspace, listener, fileContent);
                if (env != null) {
                    fileContent = Util.replaceMacro(fileContent, env);
                }
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in content of " + configFile.name + " - " + e.getMessage());
            }
        }

        ByteArrayInputStream bs = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
        target.copyFrom(bs);
        target.chmod(0640);

        return target;
    }

}