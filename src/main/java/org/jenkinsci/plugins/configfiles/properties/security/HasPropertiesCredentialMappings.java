package org.jenkinsci.plugins.configfiles.properties.security;

import java.util.List;

/**
 * Implemented by {@code Config} instances that contain properties credential
 * mappings.
 */
public interface HasPropertiesCredentialMappings {

    List<PropertiesCredentialMapping> getPropertiesCredentialMappings();

    Boolean getIsReplaceAll();

}