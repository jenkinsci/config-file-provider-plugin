package org.jenkinsci.plugins.configfiles.properties.security;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import javax.annotation.CheckForNull;

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