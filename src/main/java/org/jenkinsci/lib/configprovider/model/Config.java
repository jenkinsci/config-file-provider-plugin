/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.lib.configprovider.model;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFileUtil;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileProperty;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a particular configuration file and its content.
 * <p>
 * A Config object "belongs to" a {@link ConfigProvider} instance.
 *
 * @author domi
 */
public class Config implements Serializable, Describable<Config> {

    private final static Logger LOGGER = Logger.getLogger(Config.class.getName());

    @NonNull
    public static List<Config> getConfigsInContext(@Nullable ItemGroup itemGroup, Class<? extends Descriptor> descriptor) {

        List<Config> configs = new ArrayList<Config>();

        while (itemGroup != null) {
            if (itemGroup instanceof AbstractFolder) {

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

    @NonNull
    public static <T extends Config> T getByIdOrNull(@Nullable ItemGroup itemGroup, @NonNull String configId) {

        while (itemGroup != null) {
            if (itemGroup instanceof AbstractFolder) {
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
                continue;
            }
        }

        return null;
    }

    @NonNull
    public static <T extends Config> T getByIdOrNull(@NonNull Item item, @NonNull String configId) {
        if (item instanceof AbstractFolder) {
            // configfiles defined in the folder should be available in the context of the folder
            return (T) getByIdOrNull((ItemGroup) item, configId);
        }
        if (item != null) {
            System.out.println("try with: " + item.getParent());
            return (T) getByIdOrNull(item.getParent(), configId);
        }
        return null;
    }

    public static <T extends Config> T getByIdOrNull(@NonNull Run<?, ?> build, @NonNull String configId) {
        Config configFile = null;
        if (build.getParent() != null) {
            Object parent = build.getParent();
            if (parent instanceof Item) {
                configFile = Config.getByIdOrNull((Item) parent, configId);
            } else if (parent instanceof ItemGroup) {
                configFile = Config.getByIdOrNull((ItemGroup) parent, configId);
            } else {
                LOGGER.log(Level.SEVERE, "parent type of run not supported, parent: " + parent.getClass() + ", run: " + build);
            }
        } else {
            throw new RuntimeException("Run " + build.getDisplayName() + " has no parent");
        }
        return (T) configFile;
    }

    /**
     * a unique id along all providers!
     */
    public final String id;

    /**
     * Human readable display name that distinguishes this {@link Config} instance among
     * other {@link Config} instances.
     */
    public final String name;

    /**
     * Any note that the author of this configuration wants to associate with this.
     * Jenkins doesn't use this. Can be null.
     */
    public final String comment;

    /**
     * Content of the file as-is.
     */
    public final String content;

    /**
     * The ID of the {@link ConfigProvider} in charge of managing this configuration file
     *
     * @see ConfigProvider#getProviderId()
     * @since 2.10.0
     */
    private String providerId;

    @DataBoundConstructor
    public Config(String id, String name, String comment, String content) {
        this.id = id == null ? String.valueOf(System.currentTimeMillis()) : id;
        this.name = name;
        this.comment = comment;
        this.content = content;
    }

    public Config(@NonNull Config config) {
        this(config.id, config.name, config.comment, config.content, config.providerId);
    }

    public Config(@NonNull String id, String name, String comment, String content, @NonNull String providerId) {
        if (id == null) {
            throw new IllegalArgumentException("id can NOT be null");
        }
        if (providerId == null) {
            throw new IllegalArgumentException("providerId can NOT be null");
        }
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.content = content;
        this.providerId = providerId;
    }

    /**
     * Gets the {@link ConfigProvider} that owns and manages this config.
     *
     * @return never null.
     */
    public ConfigProvider getDescriptor() {
        throw new IllegalStateException(getClass() + " must override 'getDescriptor()' this method!");
    }

    /**
     * Alias for {@link #getDescriptor()}
     */
    public ConfigProvider getProvider() {
        return getDescriptor();

    }

    public String getProviderId() {
        return providerId;
    }

    @DataBoundSetter
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + ": id=" + id + ", name=" + name + ", providerId=" + providerId + "]";
    }

}
