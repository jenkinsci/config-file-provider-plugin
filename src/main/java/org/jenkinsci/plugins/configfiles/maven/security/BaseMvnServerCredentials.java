package org.jenkinsci.plugins.configfiles.maven.security;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.BaseCredentials;

/**
 * Base class for all credentials used to authenticate a server in the maven settings.xml.
 * 
 * @author Dominik Bartholdi (imod)
 * 
 * @see CredentialsHelper#getCredentials(hudson.model.ItemGroup)
 * @see CredentialsHelper#fillAuthentication(String, java.util.Map)
 */
public class BaseMvnServerCredentials extends BaseCredentials {

    private static final long serialVersionUID = -6110233073651846011L;

    /**
     * The id.
     */
    protected final String id;
    /**
     * The description.
     */
    protected final String description;
    /**
     * The username.
     */
    protected final String username;

    public BaseMvnServerCredentials(CredentialsScope scope, String id, String username, String description) {
        super(scope);
        this.id = id;
        this.username = username;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }
}
