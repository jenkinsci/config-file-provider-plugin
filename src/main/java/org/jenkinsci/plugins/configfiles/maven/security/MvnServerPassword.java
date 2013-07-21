package org.jenkinsci.plugins.configfiles.maven.security;

import hudson.Extension;
import hudson.util.Secret;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;

import org.jenkinsci.plugins.configfiles.Messages;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides the credentials used within a maven <code>settings.xml</code> to authenticate a server connection.
 * 
 * @author Dominik Bartholdi (imod)
 */
public class MvnServerPassword extends BaseMvnServerCredentials {

    /**
     * The password.
     */
    private final Secret password;

    /**
     * Constructor.
     * 
     * @param scope
     *            the credentials scope
     * @param id
     * @param username
     *            the username.
     * @param password
     *            the password.
     * @param description
     *            the description.
     */
    @DataBoundConstructor
    public MvnServerPassword(CredentialsScope scope, String id, String username, String password, String description) {
        super(scope, id, username, description);
        this.password = Secret.fromString(password);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    public Secret getPassword() {
        return password;
    }

    /**
     * {@inheritDoc}
     */
    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.MvnServerPassword_displayname();
        }
    }

}
