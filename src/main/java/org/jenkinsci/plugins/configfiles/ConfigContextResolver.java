package org.jenkinsci.plugins.configfiles;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.ItemGroup;
import jenkins.model.Jenkins;

/**
 * ConfigContextResolver provides a way for a plugin to specify the context ({@link ItemGroup}) used to retrieve
 * configuration files from for classes deriving from {@link ItemGroup} it defines.
 */
public abstract class ConfigContextResolver implements ExtensionPoint {

    public static ExtensionList<ConfigContextResolver> all() {
        return Jenkins.getInstance().getExtensionList(ConfigContextResolver.class);
    }
    /**
     * Optionally provides the {@link ItemGroup} from which configuration files should be retrieved for the provided
     * one.
     *
     * <p> <b>Examples of expected usage:</b>
     * <p> In promoted-builds-plugin, the itemGroup used as context for a promotion run is a
     * {@code hudson.plugins.promoted_builds.JobPropertyImpl} which is not supported by the configuration file retrieval
     * logic in {@link ConfigFiles#getByIdOrNull(ItemGroup, String)}.
     * <p> However, as the {@code hudson.plugins.promoted_builds.JobPropertyImpl} has an owner property containing the
     * promoted build, it is possible for the promoted-builds-plugin to implement this extension by returning the
     * owner's parent when the itemGroup provided to the {@link #getConfigContext(ItemGroup)} method is an instance of
     * {@code hudson.plugins.promoted_builds.JobPropertyImpl}, and null if it is of any other type.
     * <p> This will allow for the configuration file retrieval code to revert to the standard logic from the owner's
     * parent starting point.
     *
     * @param itemGroup the source {@link ItemGroup}
     * @return an {@link ItemGroup} to use to retrieve configuration, or null.
     */
    public abstract ItemGroup getConfigContext(ItemGroup itemGroup);
}
