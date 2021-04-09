package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.util.ListBoxModel;
import jenkins.mvn.GlobalSettingsProvider;
import jenkins.mvn.GlobalSettingsProviderDescriptor;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

/**
 * This provider delivers the global settings.xml to the job during job/project execution. <br>
 * <b>Important: Do not rename this class!!</b> For backward compatibility, this class is also created via reflection from the maven-plugin.
 *
 * @author Dominik Bartholdi (imod)
 */
public class MvnGlobalSettingsProvider extends GlobalSettingsProvider {

    private final static Logger LOGGER = Logger.getLogger(MvnGlobalSettingsProvider.class.getName());

    private String settingsConfigId;

    /**
     * Default constructor used to load class via reflection by the maven-plugin for backward compatibility
     */
    @Deprecated
    public MvnGlobalSettingsProvider() {
    }

    @DataBoundConstructor
    public MvnGlobalSettingsProvider(String settingsConfigId) {
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

            Config c = ConfigFiles.getByIdOrNull(build.getRootBuild(), settingsConfigId);

            if (c == null) {
                String msg = "your Apache Maven build is setup to use a global config with id " + settingsConfigId + " but can not find the config";
                listener.getLogger().println("ERROR: " + msg);
                throw new IllegalStateException(msg);
            } else {

                GlobalMavenSettingsConfig config;
                if (c instanceof GlobalMavenSettingsConfig) {
                    config = (GlobalMavenSettingsConfig) c;
                } else {
                    config = new GlobalMavenSettingsConfig(c.id, c.name, c.comment, c.content, GlobalMavenSettingsConfig.isReplaceAllDefault, null);
                }

                listener.getLogger().println("using global settings config with name " + config.name);
                listener.getLogger().println("Replacing all maven server entries not found in credentials list is " + config.getIsReplaceAll());
                if (StringUtils.isNotBlank(config.content)) {
                    try {

                        FilePath workspace = build.getWorkspace();
                        if (workspace != null) {
                            FilePath workDir = WorkspaceList.tempDir(workspace);
                            String fileContent = config.content;

                            final Map<String, StandardUsernameCredentials> resolvedCredentials = CredentialsHelper.resolveCredentials(build, config.getServerCredentialMappings(), listener);
                            final Boolean isReplaceAll = config.getIsReplaceAll();

                            if (resolvedCredentials != null && !resolvedCredentials.isEmpty()) {
                                List<String> tempFiles = new ArrayList<String>();
                                fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, resolvedCredentials, workDir, tempFiles);
                                for (String tempFile : tempFiles) {
                                    build.addAction(new CleanTempFilesAction(tempFile));
                                }
                            }

                            FilePath configurationFile = workspace.createTextTempFile("global-settings", ".xml", fileContent, false);
                            LOGGER.log(Level.FINE, "Create {0}", new Object[]{configurationFile});
                            build.getEnvironments().add(new SimpleEnvironment("MVN_GLOBALSETTINGS", configurationFile.getRemote()));

                            // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by
                            // 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
                            build.addAction(new CleanTempFilesAction(configurationFile.getRemote()));
                            return configurationFile;
                        } else {
                            listener.getLogger().println("ERROR: can't supply maven settings, workspace is null / slave seems not connected...");
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("the global settings.xml could not be supplied for the current build: " + e.getMessage());
                    }
                }
            }
        }

        return null;
    }

    @Extension(ordinal = 10)
    public static class DescriptorImpl extends GlobalSettingsProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "provided global settings.xml";
        }

        public ListBoxModel doFillSettingsConfigIdItems(@AncestorInPath ItemGroup context, @AncestorInPath Item project) {
            project.checkPermission(Item.CONFIGURE);
            
            ListBoxModel items = new ListBoxModel();
            items.add("please select", "");
            for (Config config : ConfigFiles.getConfigsInContext(context, GlobalMavenSettingsConfigProvider.class)) {
                items.add(config.name, config.id);
            }
            return items;
        }

    }

}
