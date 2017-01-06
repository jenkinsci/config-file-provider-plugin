package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

import java.util.*;

/**
 * ConfigFileStore holding config files saved on top level (Jenkins instance).
 *
 * Created by domi on 17/09/16.
 */
@Extension
public class GlobalConfigFiles extends Descriptor<GlobalConfigFiles> implements ConfigFileStore, ExtensionPoint, Describable<GlobalConfigFiles> {

    private Collection<Config> configs = new ArrayList<Config>();

    public final Descriptor<GlobalConfigFiles> getDescriptor() {
        return this;
    }

    public static GlobalConfigFiles get() {
        GlobalConfigFiles instance = Jenkins.getActiveInstance().getDescriptorList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        if (instance == null) { // TODO would be useful to have an ExtensionList.getOrFail
            throw new IllegalStateException();
        }
        return instance;
    }

    public GlobalConfigFiles() {
        super(self());
        // migrate old data storage (file per provider) into new storage (one file per scope - global scope)
        ExtensionList<ConfigProvider> allProviders = ConfigProvider.all();
        for (ConfigProvider p : allProviders) {
            for (Config c: ((AbstractConfigProviderImpl)p).getConfigs().values()) {
                Config converted = ((AbstractConfigProviderImpl) p).convert(c);
                configs.add(converted);
            }
        }
        if (configs.size() > 0) {
            // in this case we migrated data from the ld format to the new store
            // this only happens once
            save();
            for (ConfigProvider p : allProviders) {
                p.clearOldDataStorage();
            }
        } else {
            load();
        }
    }

    @Override
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
    public Collection<Config> getConfigs(Class<? extends Descriptor> descriptor) {
        List<Config> cs = new ArrayList<Config>();
        for (Config c : configs) {
            if (c.getDescriptor().getClass().equals(descriptor)) {
                cs.add(c);
            }
        }
        return cs;
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

    @Override
    public void remove(String id) {
        Config c = getById(id);
        configs.remove(c);
        save();
    }

    @Override
    public String getDisplayName() {
        return Messages.display_name();
    }
}
