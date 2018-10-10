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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

/**
 * Represents a particular configuration file and its content.
 * <p>
 * A Config object "belongs to" a {@link ConfigProvider} instance.
 *
 * @author domi
 */
@SuppressWarnings("serial")
public abstract class Config implements Serializable, Describable<Config> {

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
    @Override
    public ConfigProvider getDescriptor() {
        return (ConfigProvider) Jenkins.getActiveInstance().getDescriptorOrDie(this.getClass());
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
