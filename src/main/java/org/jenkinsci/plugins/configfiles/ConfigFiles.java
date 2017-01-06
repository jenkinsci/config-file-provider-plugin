package org.jenkinsci.plugins.configfiles;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by domi on 25/10/16.
 */
public class ConfigFiles {

    private static final Logger LOGGER = Logger.getLogger(ConfigFiles.class.getName());

    private ConfigFiles() {
    }

    private static final boolean folderPluginInstalled() {
        Plugin folderPlugin = Jenkins.getActiveInstance().getPlugin("cloudbees-folder");
        return (folderPlugin != null);
    }

    @NonNull
    public static List<Config> getConfigsInContext(@Nullable ItemGroup itemGroup, Class<? extends Descriptor> descriptor) {

        List<Config> configs = new ArrayList<Config>();

        while (itemGroup != null) {
            if (folderPluginInstalled() && itemGroup instanceof AbstractFolder) {

                final AbstractFolder<?> folder = AbstractFolder.class.cast(itemGroup);
                ConfigFileStore store = folder.getProperties().get(FolderConfigFileProperty.class);
                if (store != null) {
                    if (descriptor == null) {
                        configs.addAll(store.getConfigs());
                    } else {
                        configs.addAll(store.getConfigs(descriptor));
                    }
                }
            }
            if (itemGroup instanceof Item) {
                itemGroup = Item.class.cast(itemGroup).getParent();
            }
            if (itemGroup instanceof Jenkins) {
                // we are on top scope...
                if (descriptor == null) {
                    configs.addAll(GlobalConfigFiles.get().getConfigs());
                } else {
                    configs.addAll(GlobalConfigFiles.get().getConfigs(descriptor));
                }
                itemGroup = null;
            }
        }
        return configs;
    }

    public static <T extends Config> T getByIdOrNull(@Nullable ItemGroup itemGroup, @NonNull String configId) {

        while (itemGroup != null) {
            if (folderPluginInstalled() && itemGroup instanceof AbstractFolder) {
                final AbstractFolder<?> folder = AbstractFolder.class.cast(itemGroup);
                ConfigFileStore store = folder.getProperties().get(FolderConfigFileProperty.class);
                if (store != null) {
                    Config config = store.getById(configId);
                    if (config != null) {
                        return (T) config;
                    }
                }
            }
            if (itemGroup instanceof Item) {
                itemGroup = Item.class.cast(itemGroup).getParent();
            }
            if (itemGroup instanceof Jenkins) {
                // we are on top scope...
                return (T) GlobalConfigFiles.get().getById(configId);
            } else {
                if ((itemGroup instanceof AbstractFolder) || (itemGroup instanceof Item)) {
                    continue;
                } else {
                    throw new IllegalArgumentException("can not determine current context/parent for: " + itemGroup.getFullName() + " of type " + itemGroup.getClass());
                }
            }
        }

        return null;
    }

    public static <T extends Config> T getByIdOrNull(@NonNull Item item, @NonNull String configId) {
        if (folderPluginInstalled() && item instanceof AbstractFolder) {
            // configfiles defined in the folder should be available in the context of the folder
            return (T) getByIdOrNull((ItemGroup) item, configId);
        }
        if (item != null) {
            LOGGER.log(Level.FINE, "try with: " + item.getParent());
            return (T) getByIdOrNull(item.getParent(), configId);
        }
        return null;
    }

    public static <T extends Config> T getByIdOrNull(@NonNull Run<?, ?> build, @NonNull String configId) {
        Item parent = build.getParent();
        Config configFile;
        if (parent instanceof ItemGroup) {
            configFile = getByIdOrNull((ItemGroup) parent, configId);
        } else {
            configFile = getByIdOrNull(parent, configId);
        }

        return (T) configFile;
    }
}
