package org.jenkinsci.plugins.configfiles.folder;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;

import java.io.IOException;
import java.util.*;


public class FolderConfigFileProperty extends AbstractFolderProperty<AbstractFolder<?>> implements ConfigFileStore {

    private Collection<Config> configs = new ArrayList<Config>();

    private transient AbstractFolder<?> owner;

    /*package*/ FolderConfigFileProperty(AbstractFolder<?> owner) {
        setOwner(owner);
    }

    @Override
    public Collection<Config> getConfigs() {
        return configs;
    }

    @Override
    public Collection<Config> getConfigs(Class<? extends Descriptor> descriptor) {
        List<Config> cs = new ArrayList<Config>();
        for (Config c : configs) {
            System.out.println(c.getDescriptor().getClass()+"<->"+descriptor+" : "+c.getDescriptor().getClass().equals(descriptor));
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
    public void save(Config config) {
        configs.add(config);
        try {
            getOwner().save();
        } catch (IOException e) {
            throw new RuntimeException("failed to save config to store", e);
        }
    }

    @Override
    public void remove(String id) {
        Config c = getById(id);
        configs.remove(c);
        try {
            getOwner().save();
        } catch (IOException e) {
            throw new RuntimeException("failed to remove config from store", e);
        }
    }

    @Override
    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs() {
        Map<ConfigProvider, Collection<Config>> grouped = new HashMap<ConfigProvider, Collection<Config>>();
        for (Config c : configs) {
            Collection<Config> configs = grouped.get(c.getProvider());
            if (configs == null) {
                configs = new ArrayList<Config>();
                grouped.put(c.getProvider(), configs);
            }
            configs.add(c);
        }
        return grouped;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Override
        public String getDisplayName() {
            // nothing to be shown
            return null;
        }
    }
}
