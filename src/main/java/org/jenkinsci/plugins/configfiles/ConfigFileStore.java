package org.jenkinsci.plugins.configfiles;

import hudson.model.Descriptor;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

import java.util.Collection;
import java.util.Map;

/**
 * Created by domi on 17/09/16.
 */
public interface ConfigFileStore {
    public Collection<Config> getConfigs();

    public Collection<Config> getConfigs(Class<? extends Descriptor> descriptor);

    public Config getById(String id);

    public void save(Config config);

    public void remove(String id);

    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs();
}
