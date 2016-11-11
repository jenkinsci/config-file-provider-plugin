package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.util.ListBoxModel;
import jenkins.mvn.SettingsProvider;
import jenkins.mvn.SettingsProviderDescriptor;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFileUtil;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig.MavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

/**
 * This provider delivers the settings.xml to the job during job/project execution. <br>
 * <b>Important: Do not rename this class!!</b> For backward compatibility, this class is also created via reflection from the maven-plugin.
 *
 * @author Dominik Bartholdi (imod)
 */
public class MvnSettingsProvider extends SettingsProvider {

    private final static Logger LOGGER = Logger.getLogger(MvnSettingsProvider.class.getName());

    private String settingsConfigId;

    /**
     * Default constructor used to load class via reflection by the maven-plugin for backward compatibility
     */
    @Deprecated
    public MvnSettingsProvider() {
    }

    @DataBoundConstructor
    public MvnSettingsProvider(String settingsConfigId) {
        this.settingsConfigId = settingsConfigId;
    }

    public String getSettingsConfigId() {
        return settingsConfigId;
    }

    public void setSettingsConfigId(String settingsConfigId) {
        this.settingsConfigId = settingsConfigId;
    }

    @Override
    public FilePath supplySettings(AbstractBuild<?, ?> build, TaskListener listener) {
        if (StringUtils.isNotBlank(settingsConfigId)) {

            Config c = null;
            if (build instanceof Item) {
                c = ConfigFiles.getByIdOrNull((Item) build, settingsConfigId);
            } else if (build instanceof ItemGroup) {
                c = ConfigFiles.getByIdOrNull((ItemGroup) build, settingsConfigId);
            } else if (build.getParent() instanceof ItemGroup) {
                c = ConfigFiles.getByIdOrNull((ItemGroup) build.getParent(), settingsConfigId);
            }

            if (c == null) {
                listener.getLogger().println("ERROR: your Apache Maven build is setup to use a config with id " + settingsConfigId + " but can not find the config");
            } else {

                MavenSettingsConfig config;
                if (c instanceof MavenSettingsConfig) {
                    config = (MavenSettingsConfig) c;
                } else {
                    config = new MavenSettingsConfig(c.id, c.name, c.comment, c.content, MavenSettingsConfig.isReplaceAllDefault, null);
                }

                listener.getLogger().println("using settings config with name " + config.name);
                listener.getLogger().println("Replacing all maven server entries not found in credentials list is " + config.getIsReplaceAll());
                if (StringUtils.isNotBlank(config.content)) {
                    FilePath workDir = ManagedFileUtil.tempDir(build.getWorkspace());

                    try {

                        String fileContent = config.content;

                        final List<ServerCredentialMapping> serverCredentialMappings = config.getServerCredentialMappings();
                        final Map<String, StandardUsernameCredentials> resolvedCredentials = CredentialsHelper.resolveCredentials(build, serverCredentialMappings);
                        final Boolean isReplaceAll = config.getIsReplaceAll();

                        if (!resolvedCredentials.isEmpty()) {
                            List<String> tempFiles = new ArrayList<String>();
                            fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedCredentials, workDir, tempFiles);
                            for (String tempFile : tempFiles) {
                                build.addAction(new CleanTempFilesAction(tempFile));
                            }
                        }

                        final FilePath f = build.getWorkspace().createTextTempFile("settings", ".xml", fileContent, false);
                        LOGGER.log(Level.FINE, "Create {0}", new Object[]{f});
                        build.getEnvironments().add(new SimpleEnvironment("MVN_SETTINGS", f.getRemote()));

                        // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by
                        // 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
                        build.addAction(new CleanTempFilesAction(f.getRemote()));
                        return f;
                    } catch (Exception e) {
                        throw new IllegalStateException("the settings.xml could not be supplied for the current build: " + e.getMessage(), e);
                    }
                }
            }
        }

        return null;
    }

    @Extension(ordinal = 10)
    public static class DescriptorImpl extends SettingsProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "provided settings.xml";
        }

        public ListBoxModel doFillSettingsConfigIdItems(@AncestorInPath ItemGroup context) {
            ListBoxModel items = new ListBoxModel();
            items.add("please select", "");
            for (Config config : ConfigFiles.getConfigsInContext(context, MavenSettingsConfigProvider.class)) {
                items.add(config.name, config.id);
            }
            return items;
        }
    }

}
