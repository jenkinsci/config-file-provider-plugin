package org.jenkinsci.plugins.configfiles.properties.security;

import java.util.List;

public interface HasPropertyCredentialMappings {

    List<PropertiesCredentialMapping> getPropertiesCredentialMappings();

    Boolean getIsReplaceAll();

}