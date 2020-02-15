package org.jenkinsci.plugins.configfiles.maven.security.server;

import javax.annotation.CheckForNull;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;

/**
 * @author Dominik Bartholdi (imod)
 * 
 */
public class MavenServerIdRequirement extends DomainRequirement {

    private String serverId;

    /**
     * 
     */
    public MavenServerIdRequirement(String serverId) {
        this.serverId = serverId;
    }

    @CheckForNull
    public String getServerId() {
        return serverId;
    }

}
