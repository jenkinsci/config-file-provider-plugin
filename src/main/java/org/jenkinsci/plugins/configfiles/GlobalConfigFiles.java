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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * ConfigFileStore holding config files saved on top level (Jenkins instance).
 * <p>
 * Created by domi on 17/09/16.
 */
@Extension(ordinal = 5)
public class GlobalConfigFiles extends Descriptor<GlobalConfigFiles> implements ConfigFileStore, ExtensionPoint, Describable<GlobalConfigFiles> {

    private static Comparator<Config> COMPARATOR = new ConfigComparator();

    private static ConfigProviderComparator CONFIGPROVIDER_COMPARATOR = new ConfigProviderComparator();

    private Collection<Config> configs = new TreeSet<>(COMPARATOR);

    public final Descriptor<GlobalConfigFiles> getDescriptor() {
        return this;
    }

    public static GlobalConfigFiles get() {
        GlobalConfigFiles instance = Jenkins.getActiveInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
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
            for (Config c : ((AbstractConfigProviderImpl) p).getConfigs().values()) {
                Config converted = ((AbstractConfigProviderImpl) p).convert(c);
                configs.add(converted);
            }
        }
        if (configs.size() > 0) {
            // in this case we migrated data from the old format to the new store
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
    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs() {
        Map<ConfigProvider, Collection<Config>> grouped = new TreeMap<ConfigProvider, Collection<Config>>(CONFIGPROVIDER_COMPARATOR);
        for (Config c : configs) {
            Collection<Config> configs = grouped.get(c.getProvider());
            if (configs == null) {
                configs = new ArrayList<>();
                grouped.put(c.getProvider(), configs);
            }
            configs.add(c);
        }
        for (Map.Entry<ConfigProvider, Collection<Config>> entry :
                grouped.entrySet()) {
            List<Config> value = (List<Config>) entry.getValue();
            Collections.sort(value, ConfigByNameComparator.INSTANCE);
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
        if (id != null) {
            for (Config c : configs) {
                if (id.equals(c.id)) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public void save(Config config) {
        configs.remove(config);
        configs.add(config);
        save();
    }

    @Override
    public void remove(String id) {
        Config c = getById(id);
        if (c != null) {
            configs.remove(c);
            save();
        }
    }

    @Override
    public String getDisplayName() {
        return Messages.display_name();
    }

    private Object readResolve() {
        if (!(configs instanceof TreeSet)) {
            Collection<Config> newConfigs = new TreeSet<>(COMPARATOR);
            newConfigs.addAll(configs);
            configs = newConfigs;
        }
        return this;
    }

    private static class ConfigComparator implements Comparator<Config> {
        @Override
        public int compare(Config o1, Config o2) {
            return o1.id.compareTo(o2.id);
        }
    }
}
