package org.jenkinsci.plugins.configfiles.properties;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.properties.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.properties.security.HasPropertyCredentialMappings;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public abstract class AbstractPropertiesProvider extends AbstractConfigProviderImpl {

    @Override
    public ContentType getContentType() {
        return ContentType.DefinedType.PROPERTIES;
    }

    protected String loadTemplateContent() {
        InputStream in = null;
        try {
            in = AbstractPropertiesProvider.class.getResourceAsStream("settings-tpl.xml");
            return org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        } catch (Exception e) {
            return "myProp=myValue";
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(in);
        }
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
}