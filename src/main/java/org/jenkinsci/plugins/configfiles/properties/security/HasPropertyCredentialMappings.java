package org.jenkinsci.plugins.configfiles.properties.security;

import java.util.List;

public interface HasPropertyCredentialMappings {

    public abstract List<PropertiesCredentialMapping> getPropertiesCredentialMappings();

    public abstract Boolean getIsReplaceAll();

}