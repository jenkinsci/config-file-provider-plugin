package org.jenkinsci.plugins.configfiles.properties.security;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class PropertyKeyRequirement extends DomainRequirement {

    private String propertyKey;

    public PropertyKeyRequirement(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    @CheckForNull
    public String getPropertyKey() {
        return propertyKey;
    }

}
