package org.jenkinsci.plugins.configfiles.properties.security;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import javax.annotation.CheckForNull;

public class PropertiesRequirement extends DomainRequirement {

    private String propertyKey;

    PropertiesRequirement(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    @CheckForNull
    public String getPropertyKey() {
        return propertyKey;
    }

}
