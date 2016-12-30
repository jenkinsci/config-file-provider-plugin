package org.jenkinsci.plugins.configfiles.maven.job;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.slaves.WorkspaceList;
import hudson.util.ListBoxModel;
import jenkins.mvn.GlobalSettingsProvider;
import jenkins.mvn.GlobalSettingsProviderDescriptor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.common.CleanTempFilesAction;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
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
    @CheckForNull
    public FilePath supplySettings(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener) throws IOException, InterruptedException{
        if (StringUtils.isBlank(settingsConfigId)) {
            return null;
        }

        Config c = ConfigFiles.getByIdOrNull(run, settingsConfigId);

        PrintStream console = listener.getLogger();
        if (c == null) {
            listener.error("Maven global settings.xml with id '" + settingsConfigId + "' not found");
            return null;
        }
        if (StringUtils.isBlank(c.content)) {
            console.format("Ignore empty maven global settings.xml with id " + settingsConfigId);
            return null;
        }

        GlobalMavenSettingsConfig config;
        if (c instanceof GlobalMavenSettingsConfig) {
            config = (GlobalMavenSettingsConfig) c;
        } else {
            config = new GlobalMavenSettingsConfig(c.id, c.name, c.comment, c.content, GlobalMavenSettingsConfig.isReplaceAllDefault, null);
        }

        FilePath workspaceTmpDir = WorkspaceList.tempDir(workspace);
        workspaceTmpDir.mkdirs();

        String fileContent = config.content;

        final List<ServerCredentialMapping> serverCredentialMappings = config.getServerCredentialMappings();
        final Map<String, StandardUsernameCredentials> credentialsByMavenServerId = CredentialsHelper.resolveCredentials(run, serverCredentialMappings);
        final Boolean isReplaceAll = config.getIsReplaceAll();

        if (!credentialsByMavenServerId.isEmpty()) {

            console.println("Inject in Maven global settings.xml credentials for maven servers (replaceAll: " + config.isReplaceAll + "): " +
                    Joiner.on(", ").skipNulls().join(
                            Iterables.transform(credentialsByMavenServerId.entrySet(),
                                    new Function<Map.Entry<String, StandardUsernameCredentials>, String>() {
                                        @Override
                                        public String apply(@Nullable Map.Entry<String, StandardUsernameCredentials> entry) {
                                            return entry == null ? null: (entry.getKey() + "->" + entry.getValue());
                                        }
                                    })));

            // temporary credentials files (ssh pem files...)
            List<String> tmpCredentialsFiles = new ArrayList<>();
            try {
                fileContent = CredentialsHelper.fillAuthentication(fileContent, isReplaceAll, credentialsByMavenServerId, workspaceTmpDir, tmpCredentialsFiles);
            } catch (IOException e) {
                throw new IOException("Exception injecting credentials for maven global settings file '" + config.id + "' during '" + run + "'", e);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Exception injecting credentials for maven global settings file '" + config.id + "' during '" + run + "'", e);
            }
            for (String tmpCredentialsFile : tmpCredentialsFiles) {
                run.addAction(new CleanTempFilesAction(tmpCredentialsFile));
            }
        }

        final FilePath mavenGlobalSettingsFile = workspaceTmpDir.createTempFile("maven-global-", "-settings.xml");
        mavenGlobalSettingsFile.copyFrom(org.apache.commons.io.IOUtils.toInputStream(fileContent, Charsets.UTF_8));

        LOGGER.log(Level.FINE, "Create {0} from {1}", new Object[]{mavenGlobalSettingsFile, config.id});

        // Temporarily attach info about the files to be deleted to the build - this action gets removed from the build again by
        // 'org.jenkinsci.plugins.configfiles.common.CleanTempFilesRunListener'
        run.addAction(new CleanTempFilesAction(mavenGlobalSettingsFile.getRemote()));

        if (run instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) run;
            build.getEnvironments().add(new SimpleEnvironment("MVN_GLOBALSETTINGS", mavenGlobalSettingsFile.getRemote()));
        }

        return mavenGlobalSettingsFile;

    }

    @Extension(ordinal = 10)
    public static class DescriptorImpl extends GlobalSettingsProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "provided global settings.xml";
        }

        public ListBoxModel doFillSettingsConfigIdItems(@AncestorInPath ItemGroup context) {
            ListBoxModel items = new ListBoxModel();
            items.add("please select", "");
            for (Config config : ConfigFiles.getConfigsInContext(context, GlobalMavenSettingsConfigProvider.class)) {
                items.add(config.name, config.id);
            }
            return items;
        }

    }

}