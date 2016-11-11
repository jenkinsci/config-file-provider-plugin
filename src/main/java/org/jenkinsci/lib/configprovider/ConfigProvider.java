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
package org.jenkinsci.lib.configprovider;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;

import java.util.Collection;

/**
 * A ConfigProvider represents a configuration file (such as Maven's settings.xml) where the user can choose its actual content among several {@linkplain Config concrete contents} that are
 * pre-configured.
 * 
 * <p>
 * {@link ConfigProvider} is an extension point, and should be implemented and instantiated by each kind of configuration. This abstraction doesn't define where the configuration is placed, or
 * how/when it's used &mdash; those semantics should be introduced by a specific instance of {@link ConfigProvider}.
 * 
 * @author Dominik Bartholdi (imod)
 */
public abstract class ConfigProvider extends Descriptor<Config> implements ExtensionPoint {

    /**
     * All registered {@link ConfigProvider}s.
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Jenkins.getInstance() should never be null")
    public static ExtensionList<ConfigProvider> all() {
        return Jenkins.getInstance().getExtensionList(ConfigProvider.class);
    }

    /**
     * Lookup a {@link ConfigProvider} by its id.
     *
     * @param providerId id of the desired {@link ConfigProvider}
     * @return the {@link ConfigProvider} or {@code null} if not found
     */
    @CheckForNull
    public static ConfigProvider getByIdOrNull(@Nullable String providerId) {
        if (providerId == null || providerId.isEmpty()) {
            return null;
        }

        for (ConfigProvider provider : ConfigProvider.all()) {
            if (providerId.equals(provider.getProviderId())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * returns all the configs of this provider
     * 
     * @return collection of Configs
     */
    public abstract Collection<Config> getAllConfigs();

    /**
     * The content type of the configs this provider manages. e.g. can be used to display the content in the UI (editor).
     * 
     * @return the type. <code>null</code> if no specific formating should be supported.
     */
    public abstract ContentType getContentType();

    /**
     * Returns the config item identified by this id.
     * 
     * @param configId
     *            the id
     * @return the config with the given id
     */
    public abstract Config getConfigById(String configId);

    /**
     * Returns {@code true} if the given {@code configId} exists
     *
     * @param configId
     *            the id
     * @return the config with the given id
     * @since 2.10.0
     */
    public boolean configExists(String configId) {
        throw new AbstractMethodError(this.getClass() + " should implement configExists(String configId)");
    }

    /**
     * Whether this provider is responsible for a Config with the given Id.
     * 
     * @param configId
     *            the id to check
     * @return <code>true</code> if the provider takes responsibility
     */
    public abstract boolean isResponsibleFor(@NonNull String configId);

    /**
     * save the content of the given config.
     * 
     * @param config
     *            the config to be saved
     */
    public abstract void save(Config config);

    /**
     * Removes/deletes the config with the given Id
     * 
     * @param configId
     *            the id
     */
    public abstract void remove(String configId);

    /**
     * An ID uniquely identifying this provider, the id of each {@link Config} must start with this ID separated by a '.'!
     * 
     * @return the unique id for this provider.
     */
    public abstract String getProviderId();

    /**
     * Returns a new {@link Config} object with a unique id, starting with the id of this provider - separated by '.'. e.g. "MyCustomProvider.123456". This object is also used initialize the user
     * interface.
     * 
     * @return the new config object, ready for editing.
     * @deprecated use {@link #newConfig(String)}
     */
    @Deprecated
    public abstract Config newConfig();

    /**
     * Returns a new {@link Config} object.
     *
     * @param id desired id
     * @return the created configuration
     * @since 2.10.0
     */
    @NonNull
    public Config newConfig(@NonNull String id) {
        // concrete implementation throwing an AbstractMethodError to be backward with old ConfigProvider who only implement newConfig()
        throw new AbstractMethodError(getClass() + " MUST implement 'newConfig(String)'");
    }


}
