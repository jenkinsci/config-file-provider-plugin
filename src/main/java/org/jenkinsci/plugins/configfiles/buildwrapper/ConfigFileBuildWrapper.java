package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.configfiles.ConfigProvider;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.kohsuke.stapler.DataBoundConstructor;

public class ConfigFileBuildWrapper extends BuildWrapper {

	private List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();

	@DataBoundConstructor
	public ConfigFileBuildWrapper(List<ManagedFile> managedFiles) {
		this.managedFiles = managedFiles;
	}

	@Override
	public Environment setUp(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
			InterruptedException {
		final PrintStream logger = listener.getLogger();

		for (ManagedFile managedFile : managedFiles) {
			ConfigProvider provider = this.getProviderForConfigId(managedFile.fileId);
			Config configFile = provider.getConfigById(managedFile.fileId);

			String targetLocation = StringUtils.isBlank(managedFile.targetLocation) ? "" : managedFile.targetLocation;
			if (!targetLocation.contains(".")) {
				if (StringUtils.isBlank(targetLocation)) {
					targetLocation = configFile.name.replace(" ", "_");
				} else {
					targetLocation = targetLocation + "/" + configFile.name.replace(" ", "_");
				}
			}

			FilePath target = new FilePath(build.getWorkspace(), targetLocation);
			logger.println(Messages.console_output(configFile.name, target.toURI()));
			ByteArrayInputStream bs = new ByteArrayInputStream(configFile.content.getBytes());
			target.copyFrom(bs);
		}
		return new Environment() {
		};
	}

	private ConfigProvider getProviderForConfigId(String id) {
		if (!StringUtils.isBlank(id)) {
			for (ConfigProvider provider : ConfigProvider.all()) {
				if (provider.isResponsibleFor(id)) {
					return provider;
				}
			}
		}
		return null;
	}

	public List<ManagedFile> getManagedFiles() {
		return managedFiles;
	}

	/**
	 * Descriptor for {@link ExclusiveBuildWrapper}. Used as a singleton. The
	 * class is marked as public so that it can be accessed from views.
	 */
	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public String getDisplayName() {
			return Messages.display_name();
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		public Collection<Config> getConfigFiles() {
			ExtensionList<ConfigProvider> providers = ConfigProvider.all();
			List<Config> allFiles = new ArrayList<Config>();
			for (ConfigProvider provider : providers) {
				allFiles.addAll(provider.getAllConfigs());
			}
			return allFiles;
		}

	}
}
