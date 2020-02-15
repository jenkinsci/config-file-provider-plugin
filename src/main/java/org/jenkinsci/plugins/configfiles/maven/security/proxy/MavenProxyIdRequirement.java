package org.jenkinsci.plugins.configfiles.maven.security.proxy;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import javax.annotation.CheckForNull;

/**
 * @author Stefan Richter
 */
public class MavenProxyIdRequirement extends DomainRequirement {

	private String proxyId;

	public MavenProxyIdRequirement(String proxyId) {
		this.proxyId = proxyId;
	}

	@CheckForNull
	public String getProxyId() {
		return proxyId;
	}

}
