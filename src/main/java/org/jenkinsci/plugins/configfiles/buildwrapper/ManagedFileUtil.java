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

import jenkins.tasks.SimpleBuildWrapper;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.maven.security.HasServerCredentialMappings;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.slaves.WorkspaceList;

public class ManagedFileUtil {
    private final static Logger LOGGER = Logger.getLogger(ManagedFileUtil.class.getName());

    /**
     * TODO use 1.652 use WorkspaceList.tempDir
     * 
     * @param ws
     *            workspace of the {@link hudson.model.Build}. See {@link Build#getWorkspace()}
     * @return temporary directory, may not have been created so far
     */
    public static FilePath tempDir(FilePath ws) {
        return ws.sibling(ws.getName() + System.getProperty(WorkspaceList.class.getName(), "@") + "tmp");
    }

    /**
     * provisions (publishes) the given files to the workspace.
     * 
     * @param managedFiles
     *            the files to be provisioned
     * @param workspace
     *            target workspace
     * @param listener
     *            the listener
     * @param tempFiles
     *            temp files created by this method, these files should be deleted by the caller
     * @return a map of all the files copied, mapped to the path of the remote location, never <code>null</code>.
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException
     *             config file has not been found
     */
    public static Map<ManagedFile, FilePath> provisionConfigFiles(List<ManagedFile> managedFiles, Run<?, ?> build, FilePath workspace, TaskListener listener, List<String> tempFiles) throws IOException, InterruptedException {

        final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();
        listener.getLogger().println("provisoning config files...");

        for (ManagedFile managedFile : managedFiles) {

            Config configFile = null;
            if(build.getParent() != null){
                Object parent = build.getParent();
                if(parent instanceof Item){
                    configFile = Config.getByIdOrNull((Item) parent, managedFile.fileId);
                }else if(parent instanceof ItemGroup){
                    configFile = Config.getByIdOrNull((ItemGroup) parent, managedFile.fileId);
                }else{
                    System.out.println("build is of type: "+parent.getClass()+" : "+build);
                }
            } else {
                System.out.println("parent was null, build is of type: "+build.getClass()+" : "+build);
            }

            if (configFile == null) {
                throw new AbortException("not able to provide the following file, can't be resolved by any provider - maybe it got deleted by an administrator: " + managedFile);
            }

            FilePath workDir = tempDir(workspace);
            workDir.mkdirs();

            boolean createTempFile = StringUtils.isBlank(managedFile.targetLocation);

            FilePath target;
            if (createTempFile) {
                target = workDir.createTempFile("config", "tmp");
            } else {

                String expandedTargetLocation = managedFile.targetLocation;
                try {
                    expandedTargetLocation = build instanceof AbstractBuild ? TokenMacro.expandAll((AbstractBuild<?, ?>) build, listener, managedFile.targetLocation) : managedFile.targetLocation;
                } catch (MacroEvaluationException e) {
                    listener.getLogger().println("[ERROR] failed to expand variables in target location '" + managedFile.targetLocation + "' : " + e.getMessage());
                    expandedTargetLocation = managedFile.targetLocation;
                }

                // Should treat given path as the actual filename unless it has a trailing slash (implying a
                // directory) or path already exists in workspace as a directory.
                target = new FilePath(workspace, expandedTargetLocation);
                String immediateFileName = expandedTargetLocation.substring(expandedTargetLocation.lastIndexOf("/") + 1);

                if (immediateFileName.length() == 0 || (target.exists() && target.isDirectory())) {
                    target = new FilePath(target, configFile.name.replace(" ", "_"));
                }
            }

            // Inserts Maven server credentials if config files are Maven settings
            String fileContent = insertCredentialsInSettings(build, configFile, workDir, tempFiles);

            if (managedFile.getReplaceTokens()) {
                try {
                    fileContent = build instanceof AbstractBuild ? TokenMacro.expandAll((AbstractBuild<?, ?>) build, listener, fileContent) : fileContent;
                } catch (MacroEvaluationException e) {
                    listener.getLogger().println("[ERROR] failed to expand variables in content of " + configFile.name + " - " + e.getMessage());
                }
            }

            LOGGER.log(Level.FINE, "Create file {0} for configuration {1} mapped as {2}", new Object[] { target.getRemote(), configFile, managedFile });
            listener.getLogger().println(Messages.console_output(configFile.name, target.toURI()));
            ByteArrayInputStream bs = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
            target.copyFrom(bs);
            target.chmod(0640);
            file2Path.put(managedFile, target);
        }

        return file2Path;
    }

    private static String insertCredentialsInSettings(Run<?,?> build, Config configFile, FilePath workDir, List<String> tempFiles) throws IOException {
		String fileContent = configFile.content;
		
		if (configFile instanceof HasServerCredentialMappings) {
			HasServerCredentialMappings settings = (HasServerCredentialMappings) configFile;
			final Map<String, StandardUsernameCredentials> resolvedCredentials = CredentialsHelper.resolveCredentials(build, settings.getServerCredentialMappings());
			final Boolean isReplaceAll = settings.getIsReplaceAll();

			if (!resolvedCredentials.isEmpty()) {
				try {
					fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedCredentials, workDir, tempFiles);
				} catch (Exception exception) {
					throw new IOException("[ERROR] could not insert credentials into the settings file " + configFile, exception);
				}
			}
		}
		
		return fileContent;
	}
}
