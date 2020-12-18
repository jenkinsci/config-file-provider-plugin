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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.Messages;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class ConfigFileManager {
    private final static Logger LOGGER = Logger.getLogger(ConfigFileManager.class.getName());

    /**
     * Provisions (publishes) the given file to the workspace.
     *
     * @param configFile  the file to be provisioned
     * @param env enhanced environment to use in the variable substitution
     * @param build a build being run
     * @param workspace    target workspace
     * @param listener     the listener
     * @param tempFiles    temp files created by this method, these files should be deleted by the caller
     * @return remote location path of the provided file.
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException       config file has not been found
     */
    public static FilePath provisionConfigFile(ConfigFile configFile, @Nullable EnvVars env, Run<?, ?> build, FilePath workspace, TaskListener listener, List<String> tempFiles) throws IOException, InterruptedException {
        Config config = ConfigFiles.getByIdOrNull(build, configFile.getFileId());

        if (config == null) {
            String message = "not able to provide the file " + configFile + ", can't be resolved by any provider - maybe it got deleted by an administrator?";
            listener.getLogger().println(message);
            throw new AbortException(message);
        }

        FilePath workDir = WorkspaceList.tempDir(workspace);
        if (workDir == null) {
            throw new IllegalArgumentException("Don't configure a workspace to be the file system root, it must be in a child directory");
        }
        workDir.mkdirs();

        boolean createTempFile = StringUtils.isBlank(configFile.getTargetLocation());

        FilePath target;
        if (createTempFile) {
            target = workDir.createTempFile("config", "tmp");
        } else {

            String expandedTargetLocation = configFile.getTargetLocation();
            try {
                expandedTargetLocation = TokenMacro.expandAll(build, workspace, listener, configFile.getTargetLocation());
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in target location '" + configFile.getTargetLocation() + "' : " + e.getMessage());
                expandedTargetLocation = configFile.getTargetLocation();
            }

            // Should treat given path as the actual filename unless it has a trailing slash (implying a
            // directory) or path already exists in workspace as a directory.
            target = new FilePath(workspace, expandedTargetLocation);
            String immediateFileName = expandedTargetLocation.substring(expandedTargetLocation.lastIndexOf("/") + 1);

            if (immediateFileName.length() == 0 || (target.exists() && target.isDirectory())) {
                target = new FilePath(target, config.name.replace(" ", "_"));
            }
        }

        ConfigProvider provider = config.getDescriptor();
        String fileContent = provider.supplyContent(config, build, workDir, listener, tempFiles);

        if (configFile.isReplaceTokens()) {
            try {
                // JENKINS-57417: 'env' must be processed first, as ${x} is ambiguous between simple variable
                // references, as expected by EnvVar.expand() / Util.replaceMacro(), and parameterized token macros,
                // as expected by TokenMacro.expandAll(). Processing with expandAll() first will result in in
                // MacroEvaluationException being thrown, preventing both types of expansion.
                if (env != null) {
                    fileContent = env.expand(fileContent);
                }
                fileContent = TokenMacro.expandAll(build, workspace, listener, fileContent);
            } catch (MacroEvaluationException e) {
                listener.getLogger().println("[ERROR] failed to expand variables in content of " + config.name + " - " + e.getMessage());
            }
        }

        LOGGER.log(Level.FINE, "Create file {0} for configuration {1} mapped as {2}", new Object[]{target.getRemote(), config, configFile});
        listener.getLogger().println(Messages.console_output(config.name, target.toURI()));
        // check if empty file
        if (fileContent != null) {
            ByteArrayInputStream bs = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
            OutputStream os = target.write();
            try {
                IOUtils.copy(bs, os);
                os.flush();
            } finally {
                IOUtils.closeQuietly(bs);
                IOUtils.closeQuietly(os);
            }
        }
        target.chmod(0640);

        return target;
    }

}
