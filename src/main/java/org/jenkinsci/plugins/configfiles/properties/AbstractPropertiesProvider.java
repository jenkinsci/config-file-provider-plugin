package org.jenkinsci.plugins.configfiles.properties;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.properties.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.properties.security.HasPropertyCredentialMappings;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractPropertiesProvider extends AbstractConfigProviderImpl {

    @Override
    public ContentType getContentType() {
        return ContentType.DefinedType.PROPERTIES;
    }

    @Override
    public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
        HasPropertyCredentialMappings settings = (HasPropertyCredentialMappings) configFile;
        final Map<String, StandardUsernameCredentials> resolvedCredentials = CredentialsHelper.resolveCredentials(build, settings.getPropertiesCredentialMappings(), listener);
        final Boolean isReplaceAll = settings.getIsReplaceAll();

        String fileContent = super.supplyContent(configFile, build, workDir, listener, tempFiles);
        if (!resolvedCredentials.isEmpty()) {
            try {
                fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedCredentials);
            } catch (Exception exception) {
                throw new IOException("[ERROR] could not insert credentials into the settings file " + configFile, exception);
            }
        }
        return fileContent;
    }

    @Override
    public @NonNull List<String> getSensitiveContentForMasking(Config configFile, Run<?, ?> build) {
        HasPropertyCredentialMappings settings = (HasPropertyCredentialMappings) configFile;
        return CredentialsHelper.secretsForMasking(build, settings.getPropertiesCredentialMappings());
    }
}
