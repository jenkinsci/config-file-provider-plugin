package org.jenkinsci.plugins.configfiles.maven.security;

import org.jenkinsci.plugins.configfiles.maven.security.proxy.ProxyCredentialMapping;
import org.jenkinsci.plugins.configfiles.maven.security.server.ServerCredentialMapping;

import java.util.List;

/**
 * Implemented by {@code Config} instances that contain server credential
 * mappings (Maven settings).
 */
public interface HasCredentialMappings {

	public abstract List<ServerCredentialMapping> getServerCredentialMappings();
	public abstract Boolean getIsReplaceAll();

	public abstract List<ProxyCredentialMapping> getProxyCredentialMappings();
}