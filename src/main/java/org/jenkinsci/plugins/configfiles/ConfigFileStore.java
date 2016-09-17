package org.jenkinsci.plugins.configfiles;

import org.jenkinsci.lib.configprovider.model.Config;

import java.util.Collection;

/**
 * Created by domi on 17/09/16.
 */
public interface ConfigFileStore {
    public Collection<Config> getConfigs();

    public Config getById(String id);

    public void save(Config config);
}
