package org.jenkinsci.lib.configprovider;

import org.jenkinsci.lib.configprovider.model.Config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Partial default implementation of {@link ConfigProvider}.
 * 
 * Subtype must call the {@link #load()} method in the constructor.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractConfigProviderImpl extends ConfigProvider {

	protected Map<String, Config> configs = new HashMap<String, Config>();

	public AbstractConfigProviderImpl() {
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
	public String getProviderId() {
		return getId();
	}

	@Override
	public boolean isResponsibleFor(String configId) {
		return configId != null && configId.startsWith(getProviderId());
	}

	@Override
	public Config newConfig() {
		String id = this.getProviderId() + "." + System.currentTimeMillis();
		return new Config(id, null, null, null);
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
}
