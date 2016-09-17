package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by domi on 17/09/16.
 */
@Extension
public class GlobalConfigFiles extends GlobalConfiguration implements ConfigFileStore {

    private Collection<Config> configs = new ArrayList<Config>();

    public static GlobalConfigFiles get() {
        GlobalConfigFiles instance = GlobalConfiguration.all().get(GlobalConfigFiles.class);
        if (instance == null) { // TODO would be useful to have an ExtensionList.getOrFail
            throw new IllegalStateException();
        }
        return instance;
    }

    public GlobalConfigFiles() {

        // migrate old data storage (file per provider) into new storage (one file per scope - global scope)
        ExtensionList<ConfigProvider> allProviders = ConfigProvider.all();
        for (ConfigProvider p : allProviders) {
            for (Config c: ((AbstractConfigProviderImpl)p).getConfigs().values()) {
                Config converted = ((AbstractConfigProviderImpl) p).convert(c);
                System.out.println("converted to: "+converted.getClass());
                configs.add(converted);
            }
        }
        if (configs.size() > 0) {
            // in this case we migrated data from the ld format to the new store
            save();
            for (ConfigProvider p : allProviders) {
//                p.clearOldDataStorage();
//                p.save();
            }
        } else {
            load();
        }
    }

    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs(){
        Map<ConfigProvider, Collection<Config>> grouped = new HashMap<ConfigProvider, Collection<Config>>();
        for (Config c : configs) {
            Collection<Config> configs = grouped.get(c.getProvider());
            if(configs == null){
                configs = new ArrayList<Config>();
                grouped.put(c.getProvider(), configs);
            }
            configs.add(c);
        }
        return grouped;
    }

    @Override
    public Collection<Config> getConfigs() {
        return configs;
    }

    @Override
    public Config getById(String id) {
        for (Config c : configs) {
            if (id.equals(c.id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void save(Config config){
        configs.add(config);
        save();
    }

}
