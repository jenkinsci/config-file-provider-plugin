package org.jenkinsci.plugins.configfiles;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

import java.util.Collection;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ContentType;

public abstract class ConfigProvider implements ExtensionPoint {

	/**
	 * All registered {@link ConfigProvider}s.
	 */
	public static ExtensionList<ConfigProvider> all() {
		return Jenkins.getInstance().getExtensionList(ConfigProvider.class);
	}

	public abstract Collection<Config> getAllConfigs();

	public abstract ContentType getSupportedContentType();

	public abstract Config getConfigById(String configId);

	public abstract boolean isResponsibleFor(String configId);

	public abstract void save(Config config);

	public abstract void remove(String configId);

	/**
	 * returns a new config object with a unique id (unique along all
	 * providers!)
	 * 
	 * @return the new config object, ready for editing.
	 */
	public abstract Config newConfig();

}
