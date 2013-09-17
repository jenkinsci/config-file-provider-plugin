package org.jenkinsci.plugins.configfiles.maven.security;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.ListBoxModel;

import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

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

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath AbstractProject context) {
            return new StandardUsernameListBoxModel().withEmptySelection().withAll(allCredentials());
        } 
        
        private static List<StandardUsernameCredentials> allCredentials() {
            final List<StandardUsernameCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, Jenkins.getInstance(), /* TODO per-build auth? */ACL.SYSTEM, /* TODO restrict */Collections.<DomainRequirement>emptyList());
            System.out.println("Credentials**>"+creds.size());
            for (StandardUsernameCredentials c : creds) {
                System.out.println("-->"+c.getUsername());
            }
            return creds;
        }        

        @Override
        public String getDisplayName() {
            return "";
        }

    }
}
