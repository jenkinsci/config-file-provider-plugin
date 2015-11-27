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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Util;
import hudson.model.Describable;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

/**
 * Represents a particular configuration file and its content.
 *
 * A Config object "belongs to" a {@link ConfigProvider} instance.
 *
 * @author domi
 */
public class Config implements Serializable, Describable<Config> {

	/**
	 * Get {@link Config} by id.
	 *
	 * @param configId if of the desired {@link Config}
	 * @return desired {@link Config} or {@code null} if not found
     */
	@CheckForNull
	public static Config getByIdOrNull(@Nullable String configId) {
		if (configId == null)
			return null;
		for (ConfigProvider provider : ConfigProvider.all()) {
			if (Util.isOverridden(ConfigProvider.class, provider.getClass(), "configExists", String.class)) {
				if (provider.configExists(configId)) {
					Config config = provider.getConfigById(configId);
					config.setProviderId(provider.getProviderId());
					return config;
				}
			} else {
				// ConfigProvider implementations that doesn't yet implement "configExists(configId))"
				// may throw a RuntimeException on "getConfigById(configId)" if the config object does not exist
				try {
					Config config = provider.getConfigById(configId);
					if (config != null) {
						config.setProviderId(provider.getProviderId());
						return config;
					}
				} catch (RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Get {@link Config} by id.
	 *
	 * @param configId if of the desired {@link Config}
	 * @return desired {@link Config}, never null
	 * @throws RuntimeException if the desired config is not found
	 */
	@NonNull
	public static Config getById(@NonNull String configId) {
		if (configId == null)
			throw new IllegalArgumentException("configId can NOT be null");
		Config result = getByIdOrNull(configId);
		if (result == null)
			throw new RuntimeException("No config found for id '" + configId + "'");
		return result;
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
	 * @since 2.10.0
	 * @see ConfigProvider#getProviderId()
	 */
	private String providerId;

	@DataBoundConstructor
	public Config(String id, String name, String comment, String content) {
		this.id = id == null ? String.valueOf(System.currentTimeMillis()) : id;
		this.name = name;
		this.comment = comment;
		this.content = content;
	}

	public Config(@NonNull String id, String name, String comment, String content, @NonNull String providerId) {
		if (id == null)
			throw new IllegalArgumentException("id can NOT be null");
		if (providerId == null)
			throw new IllegalArgumentException("providerId can NOT be null");
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
		ConfigProvider result = ConfigProvider.getByIdOrNull(this.providerId);

		if (result != null)
			return result;

		// backward compatibility: config.providerId may be null (older than 2.10)
		for (ConfigProvider provider : ConfigProvider.all()) {
			if (provider.isResponsibleFor(id)) {
				return provider;
			}
		}

        throw new IllegalStateException("Unable to find the owner provider for ID="+id);
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
		return "[Config: id=" + id + ", name=" + name + ", providerId=" + providerId +"]";
	}

	public void remove() {
		getDescriptor().remove(id);
	}
}
