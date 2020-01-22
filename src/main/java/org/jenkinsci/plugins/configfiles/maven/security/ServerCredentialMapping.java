package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import hudson.model.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class ServerCredentialMapping extends AbstractDescribableImpl<ServerCredentialMapping> implements Serializable {

    private final String serverId;
    private final String credentialsId;

    @DataBoundConstructor
    public ServerCredentialMapping(String serverId, String credentialsId) {
        this.serverId = serverId;
        this.credentialsId = credentialsId;
    }

    public String getServerId() {
        return serverId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Descriptor<ServerCredentialMapping> getDescriptor() {
        return DESCRIPTOR;
    }

    private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Extension
    public static class DescriptorImpl extends Descriptor<ServerCredentialMapping> {

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String serverId) {
            AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get());
            if (_context == null || !_context.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(serverId);
            }

            List<DomainRequirement> domainRequirements = Collections.emptyList();
            if (StringUtils.isNotBlank(serverId)) {
                domainRequirements = Collections.singletonList(new MavenServerIdRequirement(serverId));
            }

            // @formatter:off
            return new StandardUsernameListBoxModel().includeAs(
                        context instanceof Queue.Task ? ((Queue.Task) context).getDefaultAuthentication() : ACL.SYSTEM, 
                        context, 
                        StandardUsernameCredentials.class, 
                        domainRequirements
                    )
                    .includeCurrentValue(serverId);
            // @formatter:on
        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }

}
