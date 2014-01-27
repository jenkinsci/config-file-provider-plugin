package org.jenkinsci.plugins.configfiles.maven.security;

import java.util.List;

/**
 * Implemented by {@code Config} instances that contain server credential
 * mappings (Maven settings).
 */
public interface HasServerCredentialMappings {

	public abstract List<ServerCredentialMapping> getServerCredentialMappings();

}