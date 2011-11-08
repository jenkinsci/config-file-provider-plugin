package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.FilePath;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

public class ManagedFileUtil {

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

	/**
	 * provisions (publishes) the given files to the workspace.
	 * 
	 * @param managedFiles
	 *            the files to be provisioned
	 * @param workSpace
	 *            target workspace
	 * @param logger
	 *            the logger
	 * @return a map of all the files copied, mapped to the path of the remote
	 *         location
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Map<ManagedFile, FilePath> provisionConfigFiles(List<ManagedFile> managedFiles, FilePath workSpace, final PrintStream logger)
			throws IOException, InterruptedException {

		final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();
		logger.println("provisoning config files...");

		for (ManagedFile managedFile : managedFiles) {
			ConfigProvider provider = getProviderForConfigId(managedFile.fileId);

			if (provider == null) {
				throw new IOException(
						"not able to resolve a provider responsible for the following file - maybe a config-file-provider plugin got deleted by an administrator: "
								+ managedFile);
			}

			Config configFile = provider.getConfigById(managedFile.fileId);
			if (configFile == null) {
				throw new IOException("not able to provide the following file, can't be resolved by any provider - maybe it got deleted by an administrator: "
						+ managedFile);
			}

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
				target = new FilePath(workSpace, targetLocation);
			} else {
				target = ManagedFileUtil.createTempFile(workSpace.getChannel());
			}

			logger.println(Messages.console_output(configFile.name, target.toURI()));
			ByteArrayInputStream bs = new ByteArrayInputStream(configFile.content.getBytes());
			target.copyFrom(bs);
			file2Path.put(managedFile, target);
		}

		return file2Path;
	}

	private static ConfigProvider getProviderForConfigId(String id) {
		if (!StringUtils.isBlank(id)) {
			for (ConfigProvider provider : ConfigProvider.all()) {
				if (provider.isResponsibleFor(id)) {
					return provider;
				}
			}
		}
		return null;
	}
}
