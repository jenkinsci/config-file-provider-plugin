package org.jenkinsci.plugins.configfiles.properties;

import hudson.Extension;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.properties.security.HasPropertyCredentialMappings;
import org.jenkinsci.plugins.configfiles.properties.security.PropertiesCredentialMapping;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertiesConfig extends Config implements HasPropertyCredentialMappings {
    private static final long serialVersionUID = 1L;

    private List<PropertiesCredentialMapping> propertiesCredentialMappings;

    public Boolean isReplaceAll = isReplaceAllDefault;
    public static final Boolean isReplaceAllDefault = Boolean.TRUE;

    @DataBoundConstructor
    public PropertiesConfig(String id, String name, String comment, String content, Boolean isReplaceAll, List<PropertiesCredentialMapping> propertiesCredentialMappings) {
        super(id, name, comment, content, PropertiesConfigProvider.class.getName());
        this.propertiesCredentialMappings = propertiesCredentialMappings == null ? new ArrayList<PropertiesCredentialMapping>() : propertiesCredentialMappings;
        this.isReplaceAll = (null == isReplaceAll) ? isReplaceAllDefault : isReplaceAll;
    }

    @Override
    public List<PropertiesCredentialMapping> getPropertiesCredentialMappings() {
        return propertiesCredentialMappings == null ? new ArrayList<PropertiesCredentialMapping>() : propertiesCredentialMappings;
    }

    @Override
    public Boolean getIsReplaceAll() {
        return isReplaceAll;
    }

    @Extension(ordinal = 190)
    public static class PropertiesConfigProvider extends AbstractPropertiesProvider {

        public PropertiesConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.PROPERTIES;
        }

        @Override
        public String getDisplayName() {
            return Messages.properties_provider_description();
        }

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new PropertiesConfig(id,
                    Messages.PropertiesConfig_SettingsName(),
                    Messages.PropertiesConfig_SettingsComment(),
                    "",
                    PropertiesConfig.isReplaceAllDefault,
                    Collections.emptyList());
        }

    }

}
