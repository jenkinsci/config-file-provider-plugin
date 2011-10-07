package org.jenkinsci.plugins.configfiles.buildstep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * LibraryBuildStep {@link Builder}.
 * <p>
 * A project that uses this builder can choose a build step from a list of
 * predefined config files that are uses as command line scripts. The hash-bang
 * sequence at the beginning of each file is used to determine the interpreter.
 * <p>
 * 
 * @author Norman Baumann
 * @author domi (imod)
 */
public class LibraryBuildStep extends Builder {

	private static Logger log = Logger.getLogger(LibraryBuildStep.class.getName());

	private final String buildStepId;
	private final String[] buildStepArgs;

	/**
	 * The constructor requires 2 arguments: (a) the buildStepId which is the Id
	 * of the config file, and (b) list of arguments specified as buildStepargs.
	 */
	@DataBoundConstructor
	public LibraryBuildStep(String buildStepId, String[] buildStepArgs) {
		this.buildStepId = buildStepId;
		this.buildStepArgs = buildStepArgs;
	}

	public String getBuildStepId() {
		return buildStepId;
	}

	public String[] getBuildStepArgs() {
		return buildStepArgs;
	}

	private Launcher getLastBuiltLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) {
		AbstractProject project = build.getProject();
		Node lastBuiltOn = project.getLastBuiltOn();
		Launcher lastBuiltLauncher = launcher;
		if (lastBuiltOn != null) {
			lastBuiltLauncher = lastBuiltOn.createLauncher(listener);
		}

		return lastBuiltLauncher;
	}

	/**
	 * Perform the build step on the execution host.
	 * <p>
	 * This method overrides the default execution method of a Builder. It
	 * generates a temporary file and copies the content of the predefined
	 * config file (by using the buildStepId) into it. It then copies this file
	 * into the workspace directory of the execution host and executes it.
	 */
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		boolean returnValue = true;
		Config buildStepConfig = getDescriptor().getBuildStepConfigById(buildStepId);
		if (buildStepConfig == null) {
			listener.getLogger().println("Cannot find Build Step with Id \"" + buildStepId + "\". Are you sure it exists?");
			return false;
		}
		listener.getLogger().println("Starting build step \"" + buildStepId + "\"");
		try {
			FilePath workingDir = build.getModuleRoot();
			EnvVars env = build.getEnvironment(listener);
			Launcher lastBuiltLauncher = getLastBuiltLauncher(build, launcher, listener);
			String data = buildStepConfig.content;

			/*
			 * Create local temporary file and write script code into it
			 */
			File tempFile = File.createTempFile("build_step_template", ".sh");
			tempFile.deleteOnExit();
			BufferedWriter tempFileWriter = new BufferedWriter(new FileWriter(tempFile));
			tempFileWriter.write(data);
			tempFileWriter.close();

			/*
			 * Analyze interpreter line (and change to desired interpreter)
			 */
			ArgumentListBuilder args = new ArgumentListBuilder();
			String interpreter = new String("bash");
			if (data.startsWith("#!")) {
				String interpreterLine = data.substring(2, data.indexOf("\n"));
				String[] interpreterElements = interpreterLine.split("\\s+");
				// Add interpreter to arguments list
				interpreter = interpreterElements[0];
				args.add(interpreter);
				listener.getLogger().println("Using custom interpreter: " + interpreterLine);
				// Add addition parameter to arguments list
				for (int i = 1; i < interpreterElements.length; i++) {
					args.add(interpreterElements[i]);
				}
				args.add(tempFile.getName());
			} else {
				args.add(interpreter, tempFile.getName());
			}

			// Add additional parameters set by user
			if (buildStepArgs != null) {
				final VariableResolver<String> variableResolver = build.getBuildVariableResolver();
				for (String arg : buildStepArgs) {
					args.add(resolveVariable(variableResolver, arg));
				}
			}

			/*
			 * Copying temporary file to remote execution host
			 */
			FilePath source = new FilePath(tempFile);
			FilePath dest = new FilePath(Computer.currentComputer().getChannel(), workingDir + "/" + tempFile.getName());

			listener.getLogger().println("Copying temporary file to " + Computer.currentComputer().getHostName() + ":" + workingDir + "/" + tempFile.getName());
			source.copyTo(dest);

			/*
			 * Execute command remotely
			 */
			listener.getLogger().println("Executing temp file \"" + tempFile.getPath() + "\"");
			int r = lastBuiltLauncher.launch().cmds(args).envs(env).stderr(listener.getLogger()).stdout(listener.getLogger()).pwd(workingDir).join();
			returnValue = (r == 0);

		} catch (IOException e) {
			Util.displayIOException(e, listener);
			e.printStackTrace(listener.fatalError("Cannot create temporary build step \"" + buildStepConfig.name + "\""));
			returnValue = false;
		} catch (Exception e) {
			e.printStackTrace(listener.fatalError("Caught exception while loading build step \"" + buildStepConfig.name + "\""));
			returnValue = false;
		}

		listener.getLogger().println("Leaving build step \"" + buildStepConfig.name + "\"");
		return returnValue;
	}

	// Overridden for better type safety.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link LibraryBuildStep}.
	 */
	@Extension(ordinal = 50)
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		final Logger logger = Logger.getLogger(LibraryBuildStep.class.getName());

		/**
		 * Performs on-the-fly validation of the form field 'buildStepId'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckBuildStepId(@QueryParameter String value) throws IOException, ServletException {
			logger.log(Level.WARNING, "in doCheckBuildStepId '" + value + "'");
			if (value == null || value.length() == 0) {
				return FormValidation.error("Please choose a build script from the drop down box.");
			}
			return FormValidation.ok();
		}

		/**
		 * Enables this builder for all kinds of projects.
		 */
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return org.jenkinsci.plugins.configfiles.Messages.buildstep_name();
		}

		/**
		 * Return all config files (templates) that the user can choose from
		 * when creating a build step.
		 * 
		 * @return A collection of config files of type
		 *         {@link BuildStepConfigProvider}.
		 */
		public Collection<Config> getAvailableBuildTemplates() {
			List<Config> allConfigs = new ArrayList(getBuildStepConfigProvider().getAllConfigs());
			Collections.sort(allConfigs, new Comparator<Config>() {
				public int compare(Config o1, Config o2) {
					return o1.name.compareTo(o2.name);
				}
			});
			return allConfigs;
		}

		/**
		 * Returns a Config object for a given config file Id.
		 * 
		 * @param id
		 *            The Id of a config file.
		 * @return If Id can be found a Config object that represents the given
		 *         Id is returned. Otherwise null.
		 */
		public Config getBuildStepConfigById(String id) {
			return getBuildStepConfigProvider().getConfigById(id);
		}

		private BuildStepConfigProvider getBuildStepConfigProvider() {
			ExtensionList<ConfigProvider> providers = ConfigProvider.all();
			for (ConfigProvider provider : providers) {
				if (provider instanceof BuildStepConfigProvider) {
					return (BuildStepConfigProvider) provider;
				}
			}
			// should never happen
			return null;
		}

		/**
		 * Creates a new instance of LibraryBuildStep.
		 * 
		 * @param req
		 *            The web request as initialized by the user.
		 * @param json
		 *            A JSON object representing the users input.
		 * @return A LibraryBuildStep instance.
		 */
		@Override
		public LibraryBuildStep newInstance(StaplerRequest req, JSONObject json) {
			logger.log(Level.WARNING, "New instance of LibraryBuildStep requested with JSON data:");
			logger.log(Level.WARNING, json.toString(2));

			String id = json.getString("buildStepId");
			if (json.has("buildStepArgs") == true) {
				boolean isArray = false;
				try {
					// read with wrong type
					json.getJSONObject("buildStepArgs");
				} catch (Exception e) {
					isArray = true;
				}

				if (isArray) {
					JSONArray argsObj = json.getJSONArray("buildStepArgs");
					Iterator<JSONObject> arguments = argsObj.iterator();
					String[] args = new String[argsObj.size()];
					int i = 0;
					while (arguments.hasNext()) {
						args[i++] = arguments.next().getString("arg");
					}
					return new LibraryBuildStep(id, args);
				} else {
					String[] args = new String[1];
					args[0] = json.getJSONObject("buildStepArgs").getString("arg");
					return new LibraryBuildStep(id, args);
				}
			} else {
				return new LibraryBuildStep(id, null);
			}
		}
	}

	/**
	 * Checks whether the given parameter is a build parameter and if so,
	 * returns the value of it.
	 * 
	 * @param variableResolver
	 *            resolver to be used
	 * @param potentalVariable
	 *            the potential variable string. The string will be treated as
	 *            variable, if it follows this pattern: ${XXX}
	 * @return value of the build parameter or the origin passed string
	 */
	public static String resolveVariable(VariableResolver<String> variableResolver, String potentalVariable) {
		String value = potentalVariable;
		if (potentalVariable != null) {
			if (potentalVariable.startsWith("${") && potentalVariable.endsWith("}")) {
				value = potentalVariable.substring(2, potentalVariable.length() - 1);
				value = variableResolver.resolve(value);
				log.log(Level.FINE, "resolve " + potentalVariable + " to " + value);
			}
		}
		return value;
	}
}
