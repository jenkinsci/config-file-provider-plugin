/**
 * 
 */
package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Builds the environment to provide config files. It copies the configured
 * files to the required locations and deletes all temp files after execution.
 * 
 * @author domi
 * 
 */
public class ManagedFilesEnvironment extends Environment {

	private final Map<ManagedFile, FilePath> file2Path;

	public ManagedFilesEnvironment(Map<ManagedFile, FilePath> file2Path) {
		this.file2Path = file2Path;
	}

	@Override
	public void buildEnvVars(Map<String, String> env) {
		for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
			ManagedFile mf = entry.getKey();
			FilePath fp = entry.getValue();
			if (!StringUtils.isBlank(mf.variable)) {
				env.put(mf.variable, fp.getRemote());
			}
		}
	}

	@Override
	public boolean tearDown(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
		// delete the temporary files
		for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
			ManagedFile mf = entry.getKey();
			FilePath fp = entry.getValue();

			// we only created temp files if there was no targetLocation
			// given
			if (StringUtils.isBlank(mf.targetLocation)) {
				if (fp != null && fp.exists()) {
					fp.delete();
				}
			}
		}

		return true;
	}

}
