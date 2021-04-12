package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.*;
import hudson.security.Permission;
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

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @AncestorInPath Item projectOrFolder, @QueryParameter String serverId) {
            List<Permission> permsToCheck = projectOrFolder == null ? Arrays.asList(Jenkins.ADMINISTER) : Arrays.asList(Item.EXTENDED_READ, CredentialsProvider.USE_ITEM);
            AccessControlled contextToCheck = projectOrFolder == null ? Jenkins.get() : projectOrFolder;
            
            // If we're on the global page and we don't have administer permission or if we're in a project or folder 
            // and we don't have permission to use credentials and extended read in the item
            if (permsToCheck.stream().anyMatch( per -> !contextToCheck.hasPermission(per))) {
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
