package org.jenkinsci.plugins.configfiles;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ContentType;

public abstract class AbstractConfigProvider extends ConfigProvider implements Saveable {

	protected final String ID_PREFIX = this.getClass().getSimpleName() + ".";

	protected Map<String, Config> configs = new HashMap<String, Config>();

	public AbstractConfigProvider() {
		load();
	}

	@Override
	public Collection<Config> getAllConfigs() {
		return Collections.unmodifiableCollection(configs.values());
	}

	@Override
	public Config getConfigById(String configId) {
		return configs.get(configId);
	}

	@Override
	public abstract ContentType getSupportedContentType();

	@Override
	public boolean isResponsibleFor(String configId) {
		return configId != null && configId.startsWith(ID_PREFIX);
	}

	@Override
	public Config newConfig() {
		String id = ID_PREFIX + System.currentTimeMillis();
		return new Config(id, null, null, null, this.getSupportedContentType().name());
	}

	@Override
	public void remove(String configId) {
		configs.remove(configId);
		this.save();
	}

	@Override
	public void save(Config config) {
		configs.put(config.id, config);
		this.save();
	}

	/**
	 * @see hudson.model.Saveable#save()
	 */
	public void save() {
		if (BulkChange.contains(this))
			return;
		try {
			getConfigXml().write(this);
			SaveableListener.fireOnChange(this, getConfigXml());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void load() {
		XmlFile xml = getConfigXml();
		if (xml.exists()) {
			try {
				xml.unmarshal(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected XmlFile getConfigXml() {
		return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getInstance().getRootDir(), this.getXmlFileName()));
	}

	protected abstract String getXmlFileName();

}
