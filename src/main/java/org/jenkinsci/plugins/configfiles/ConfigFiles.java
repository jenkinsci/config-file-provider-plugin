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
import org.jenkinsci.lib.configprovider.model.ConfigFileManager;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        Plugin folderPlugin = Jenkins.get().getPlugin("cloudbees-folder");
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
        StoreAndConfig result = getStoreAndConfigOrNull(itemGroup, configId);
        return result == null ? null : (T) result.config;
    }

    /**
     * Used to get hold on a single configuration in the given context.
     * The configuration will be looked up for from the current context (item, e.g. {@link AbstractFolder} or {@link Jenkins}
     * if not within a folder) until a configuration with the given id was found.
     *
     * @param item context to start the lookup from
     * @param configId  id of the configuration to search for
     * @param <T>       expected type of the returned configuration item.
     * @return <code>null</code> if no configuration was found
     * @throws IllegalArgumentException if while walking up the tree, one of the parents is not either of type {@link AbstractFolder}, {@link Item} or {@link Jenkins}
     */
    public static <T extends Config> T getByIdOrNull(@NonNull Item item, @NonNull String configId) {
        StoreAndConfig result = getStoreAndConfigOrNull(item, configId);
        return result == null ? null : (T) result.config;
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
        StoreAndConfig result = getStoreAndConfigOrNull(build, configId);
        return result == null ? null : (T) result.config;
    }

    /**
     * Records that the given build actually used the configuration file with the given id.
     * The usage is recorded against the very store (global or folder) that owns the
     * configuration resolved for the context of the given build, i.e. the same store that
     * {@link #getByIdOrNull(Run, String)} would resolve the configuration from.
     * <p>
     * This should only be called when the configuration file is genuinely used/provisioned for
     * the build - not when it is merely looked up to be displayed, edited or validated.
     *
     * @param build    the build that used the configuration file
     * @param configId id of the configuration file that was used
     */
    public static void jobUsed(@NonNull Run<?, ?> build, @NonNull String configId) {
        if (!ConfigFileManager.isUsageTrackingEnabled()) {
            LOGGER.log(Level.FINEST, "Usage tracking is disabled, not tracking usage of configuration file {0}", configId);
            return;
        }
        StoreAndConfig result = getStoreAndConfigOrNull(build, configId);
        if (result != null) {
            result.store.trackUsage(configId, build);
        } else {
            LOGGER.log(Level.FINE, "Could not track usage of configuration file {0}, not found in the context of {1}", new Object[]{configId, build});
        }
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

    /**
     * Walks up the context tree (folder by folder) starting at the given item group, until a
     * configuration file with the given id is found, returning both the configuration and the
     * {@link ConfigFileStore} it was found in (i.e. its location).
     *
     * @param itemGroup context to start the lookup from
     * @param configId  id of the configuration to search for
     * @return <code>null</code> if no configuration was found
     * @throws IllegalArgumentException if while walking up the tree, one of the parents is not either of type {@link AbstractFolder}, {@link Item} or {@link Jenkins}
     */
    private static StoreAndConfig getStoreAndConfigOrNull(@Nullable ItemGroup itemGroup, @NonNull String configId) {

        while (itemGroup != null) {
            itemGroup = resolveItemGroup(itemGroup);
            if (folderPluginInstalled() && itemGroup instanceof AbstractFolder) {
                final AbstractFolder<?> folder = AbstractFolder.class.cast(itemGroup);
                ConfigFileStore store = folder.getProperties().get(FolderConfigFileProperty.class);
                if (store != null) {
                    Config config = store.getById(configId);
                    if (config != null) {
                        return new StoreAndConfig(store, config);
                    }
                }
            }
            if (itemGroup instanceof Item) {
                itemGroup = Item.class.cast(itemGroup).getParent();
            }
            if (itemGroup instanceof Jenkins) {
                // we are on top scope...
                ConfigFileStore store = GlobalConfigFiles.get();
                Config config = store.getById(configId);
                return config != null ? new StoreAndConfig(store, config) : null;
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

    private static StoreAndConfig getStoreAndConfigOrNull(@NonNull Item item, @NonNull String configId) {
        if (folderPluginInstalled() && item instanceof AbstractFolder) {
            // configfiles defined in the folder should be available in the context of the folder
            return getStoreAndConfigOrNull((ItemGroup) item, configId);
        }
        if (item != null) {
            LOGGER.log(Level.FINE, "try with: " + item.getParent());
            return getStoreAndConfigOrNull(item.getParent(), configId);
        }
        return null;
    }

    private static StoreAndConfig getStoreAndConfigOrNull(@NonNull Run<?, ?> build, @NonNull String configId) {
        Item parent = build.getParent();
        if (parent instanceof ItemGroup) {
            return getStoreAndConfigOrNull((ItemGroup) parent, configId);
        } else {
            return getStoreAndConfigOrNull(parent, configId);
        }
    }

    /** Holds a resolved configuration together with the store (location) it was found in. */
    private static final class StoreAndConfig {
        private final ConfigFileStore store;
        private final Config config;

        private StoreAndConfig(ConfigFileStore store, Config config) {
            this.store = store;
            this.config = config;
        }
    }
}
