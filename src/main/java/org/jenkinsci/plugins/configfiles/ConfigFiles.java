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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central class to access configuration files
 */
public class ConfigFiles {

    private static final Logger LOGGER = Logger.getLogger(ConfigFiles.class.getName());

    private ConfigFiles() {
    }

    private static final boolean folderPluginInstalled() {
        Plugin folderPlugin = Jenkins.getActiveInstance().getPlugin("cloudbees-folder");
        return (folderPlugin != null);
    }

    /**
     * Lists all configurations of the given type which are visible in the context of the provided item group.
     * e.g. if itemGroup is of type {@link AbstractFolder} or within an {@link AbstractFolder}, then this method
     * will list all configurations in that folder and in all parent folders up (and including) all configurations on jenkins top level.
     * <p>
     * This method is typically used to display all available options in the UI.
     *
     * @param itemGroup  the context
     * @param descriptor configuration type
     * @return a list of configuration items of the requested type, visible in the provided context.
     */
    @NonNull
    public static List<Config> getConfigsInContext(@Nullable ItemGroup itemGroup, Class<? extends Descriptor> descriptor) {

        List<Config> configs = new ArrayList<Config>();

        while (itemGroup != null) {
            itemGroup = resolveItemGroup(itemGroup);
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

        Collections.sort(configs, ConfigByNameComparator.INSTANCE);
        return configs;
    }

    /**
     * Used to get hold on a single configuration in the given context.
     * The configuration will be looked up for from the current context (itemGroup, e.g. {@link AbstractFolder} or {@link Jenkins}
     * if not within a folder) until a configuration with the given id was found.
     *
     * @param itemGroup context to start the lookup from
     * @param configId  id of the configuration to search for
     * @param <T>       expected type of the returned configuration item.
     * @return <code>null</code> if no configuration was found
     * @throws IllegalArgumentException if while walking up the tree, one of the parents is not either of type {@link AbstractFolder}, {@link Item} or {@link Jenkins}
     */
    public static <T extends Config> T getByIdOrNull(@Nullable ItemGroup itemGroup, @NonNull String configId) {

        while (itemGroup != null) {
            itemGroup = resolveItemGroup(itemGroup);
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

    /**
     * Used to get hold on a single configuration in the given context.
     * The configuration will be looked up for from the current context (itemGroup, e.g. {@link AbstractFolder} or {@link Jenkins}
     * if not within a folder) until a configuration with the given id was found.
     *
     * @param itemGroup context to start the lookup from
     * @param configId  id of the configuration to search for
     * @param <T>       expected type of the returned configuration item.
     * @return <code>null</code> if no configuration was found
     * @throws IllegalArgumentException if while walking up the tree, one of the parents is not either of type {@link AbstractFolder}, {@link Item} or {@link Jenkins}
     */
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

    /**
     * Used to get hold on a single configuration in the given context.
     * The configuration will be looked up for from the current context (itemGroup, e.g. {@link AbstractFolder} or {@link Jenkins}
     * if not within a folder) until a configuration with the given id was found.
     * <p>
     * Usually used to access the configuration during a run/job execution.
     *
     * @param build    active to start the lookup from
     * @param configId id of the configuration to search for
     * @param <T>      expected type of the returned configuration item.
     * @return <code>null</code> if no configuration was found
     * @throws IllegalArgumentException if while walking up the tree, one of the parents is not either of type {@link AbstractFolder}, {@link Item} or {@link Jenkins}
     */
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

    private static ItemGroup resolveItemGroup(ItemGroup itemGroup) {
        for (ConfigContextResolver resolver : ConfigContextResolver.all()) {
            ItemGroup resolvedItemGroup = resolver.getConfigContext(itemGroup);
            if (resolvedItemGroup != null) {
                return resolvedItemGroup;
            }
        }
        return itemGroup;
    }
}
