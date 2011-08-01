/**
 * 
 */
package org.jenkinsci.plugins.configfiles.buildwrapper;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author domi
 * 
 */
public class ManagedFile {

	public final String fileId;
	public final String targetLocation;

	@DataBoundConstructor
	public ManagedFile(String fileId, String targetLocation) {
		this.fileId = fileId;
		this.targetLocation = targetLocation;
	}

}
