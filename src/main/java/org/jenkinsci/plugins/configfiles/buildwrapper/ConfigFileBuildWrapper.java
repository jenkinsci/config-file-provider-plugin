/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
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

		final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();

		for (ManagedFile managedFile : managedFiles) {
			ConfigProvider provider = this.getProviderForConfigId(managedFile.fileId);
			Config configFile = provider.getConfigById(managedFile.fileId);

			boolean isTargetGiven = !StringUtils.isBlank(managedFile.targetLocation);

			FilePath target = null;
			if (isTargetGiven) {
				String targetLocation = managedFile.targetLocation;
				if (!targetLocation.contains(".")) {
					if (StringUtils.isBlank(targetLocation)) {
						targetLocation = configFile.name.replace(" ", "_");
					} else {
						targetLocation = targetLocation + "/" + configFile.name.replace(" ", "_");
					}
				}
				target = new FilePath(build.getWorkspace(), targetLocation);
			} else {
				target = ConfigFileBuildWrapper.createTempFile(build.getWorkspace().getChannel());
			}

			logger.println(Messages.console_output(configFile.name, target.toURI()));
			ByteArrayInputStream bs = new ByteArrayInputStream(configFile.content.getBytes());
			target.copyFrom(bs);
			file2Path.put(managedFile, target);
		}
		return new Environment() {

			@Override
			public void buildEnvVars(Map<String, String> env) {
				for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
					ManagedFile mf = entry.getKey();
					FilePath fp = entry.getValue();
					if (!StringUtils.isBlank(mf.variable)) {
						env.put(mf.variable, fp.getRemote());
					}
				}
			}

			@Override
			public boolean tearDown(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				// delete the temporary files
				for (Map.Entry<ManagedFile, FilePath> entry : file2Path.entrySet()) {
					ManagedFile mf = entry.getKey();
					FilePath fp = entry.getValue();

					// we only created temp files if there was no targetLocation
					// given
					if (StringUtils.isBlank(mf.targetLocation)) {
						if (fp != null && fp.exists()) {
							fp.delete();
						}
					}
				}

				return true;
			}
		};
	}

	/**
	 * creates a tmp file on the given channel
	 */
	public static FilePath createTempFile(VirtualChannel channel) throws IOException, InterruptedException {
		return channel.call(new Callable<FilePath, IOException>() {
			public FilePath call() throws IOException {
				final File tmpTarget = File.createTempFile("config", "tmp");
				return new FilePath(tmpTarget);
			}

			private static final long serialVersionUID = 1L;
		});
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
