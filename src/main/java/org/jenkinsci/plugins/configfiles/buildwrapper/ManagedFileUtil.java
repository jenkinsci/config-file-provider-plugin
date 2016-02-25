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
import hudson.model.AbstractBuild;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.maven.security.HasServerCredentialMappings;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class ManagedFileUtil {

    // TODO move to WorkspaceList
    private static FilePath tempDir(FilePath ws) {
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
     * @return a map of all the files copied, mapped to the path of the remote location, never <code>null</code>.
     * @throws IOException
     * @throws InterruptedException
     * @throws AbortException config file has not been found
     */
    public static Map<ManagedFile, FilePath> provisionConfigFiles(List<ManagedFile> managedFiles, Run<?,?> build, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {

        final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();
        listener.getLogger().println("provisoning config files...");

        for (ManagedFile managedFile : managedFiles) {

            Config configFile = Config.getByIdOrNull(managedFile.fileId);
            if (configFile == null) {
                throw new AbortException("not able to provide the following file, can't be resolved by any provider - maybe it got deleted by an administrator: " + managedFile);
            }

            boolean createTempFile = StringUtils.isBlank(managedFile.targetLocation);

            FilePath target;
            if (createTempFile) {
                FilePath tempDir = tempDir(workspace);
                tempDir.mkdirs();
                target = tempDir.createTempFile("config", "tmp");
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
                String immediateFileName = expandedTargetLocation.substring(
                		expandedTargetLocation.lastIndexOf("/")+1);

                if (immediateFileName.length() == 0 || (target.exists() && target.isDirectory())){
                	target = new FilePath(target,configFile.name.replace(" ", "_"));
                }
            }
            
            // Inserts Maven server credentials if config files are Maven settings
            String fileContent = insertCredentialsInSettings(build, configFile);


            listener.getLogger().println(Messages.console_output(configFile.name, target.toURI()));
            ByteArrayInputStream bs = new ByteArrayInputStream(fileContent.getBytes());
            target.copyFrom(bs);
            target.chmod(0640);
            file2Path.put(managedFile, target);
        }

        return file2Path;
    }

    private static String insertCredentialsInSettings(Run<?,?> build, Config configFile) throws IOException {
		String fileContent = configFile.content;
		
		if (configFile instanceof HasServerCredentialMappings) {
			HasServerCredentialMappings settings = (HasServerCredentialMappings) configFile;
			final Map<String, StandardUsernameCredentials> resolvedCredentials = CredentialsHelper.resolveCredentials(build.getParent(), settings.getServerCredentialMappings());
			final Boolean isReplaceAll = settings.getIsReplaceAll();

			if (!resolvedCredentials.isEmpty()) {
				try {
					fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedCredentials);
				} catch (Exception exception) {
					throw new IOException("[ERROR] could not insert credentials into the settings file", exception);
				}
			}
		}
		
		return fileContent;
	}
}
