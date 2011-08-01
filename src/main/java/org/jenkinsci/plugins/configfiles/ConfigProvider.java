package org.jenkinsci.plugins.configfiles;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

import java.util.Collection;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ConfigDescription;
import org.jenkinsci.plugins.configfiles.model.ContentType;

public abstract class ConfigProvider implements ExtensionPoint {

	/**
	 * All registered {@link ConfigProvider}s.
	 */
	public static ExtensionList<ConfigProvider> all() {
		return Jenkins.getInstance().getExtensionList(ConfigProvider.class);
	}

	public abstract Collection<Config> getAllConfigs();

	public abstract ConfigDescription getConfigDescription();

	public abstract ContentType getContentType();

	public abstract Config getConfigById(String configId);

	public abstract boolean isResponsibleFor(String configId);

	public abstract void save(Config config);

	public abstract void remove(String configId);

	/**
	 * An ID uniquely identifying this provider, the id of each {@link Config}
	 * must start with this ID separated by a '.'!
	 * 
	 * @return the unique id for this provider.
	 */
	public abstract String getProviderId();

	/**
	 * Returns a new {@link Config} object with a unique id, starting with the
	 * id of this provider - separated by '.'. e.g. "MyCustomProvider.123456".
	 * This object is also used initialize the user interface.
	 * 
	 * @return the new config object, ready for editing.
	 */
	public abstract Config newConfig();

}
