package org.jenkinsci.plugins.configfiles.maven.security;

import java.util.List;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;

public class ServerCredentialMapping extends AbstractDescribableImpl<ServerCredentialMapping> {

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
        return DESCIPTOR;
    }

    private static final DescriptorImpl DESCIPTOR = new DescriptorImpl();

    @Extension
    public static class DescriptorImpl extends Descriptor<ServerCredentialMapping> {

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath AbstractProject<?, ?> context, @QueryParameter String serverId) {
            if (context == null || !context.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> validCredentials = CredentialsHelper.findValidCredentials(serverId);
            return new StandardUsernameListBoxModel().withEmptySelection().withAll(validCredentials);
        }

        @Override
        public String getDisplayName() {
            return "";
        }

    }
}
