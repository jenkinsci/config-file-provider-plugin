package org.jenkinsci.plugins.configfiles.maven.security.proxy;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ProxyCredentialMapping extends AbstractDescribableImpl<ProxyCredentialMapping> implements Serializable {

	private final String proxyId;
	private final String credentialsId;

	@DataBoundConstructor
	public ProxyCredentialMapping(String proxyId, String credentialsId) {
		this.proxyId = proxyId;
		this.credentialsId = credentialsId;
	}

	public String getProxyId() {
		return proxyId;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	public Descriptor<ProxyCredentialMapping> getDescriptor() {
		return DESCRIPTOR;
	}

	private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@Extension
	public static class DescriptorImpl extends Descriptor<ProxyCredentialMapping> {

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String proxyId) {
			AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get());
			if (_context == null || !_context.hasPermission(Item.CONFIGURE)) {
				return new StandardUsernameListBoxModel().includeCurrentValue(proxyId);
			}

			List<DomainRequirement> domainRequirements = Collections.emptyList();
			if (StringUtils.isNotBlank(proxyId)) {
				domainRequirements = Collections.singletonList(new MavenProxyIdRequirement(proxyId));
			}

			return new StandardUsernameListBoxModel().includeAs(
					context instanceof Queue.Task ? ((Queue.Task) context).getDefaultAuthentication() : ACL.SYSTEM,
					context,
					StandardUsernameCredentials.class,
					domainRequirements
			)
					.includeCurrentValue(proxyId);
		}

		@Override
		@Nonnull
		public String getDisplayName() {
			return "";
		}
	}

}
