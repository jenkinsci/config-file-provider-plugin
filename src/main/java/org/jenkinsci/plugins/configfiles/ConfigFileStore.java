package org.jenkinsci.plugins.configfiles;

import hudson.model.Descriptor;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.Run;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

import java.util.Collection;
import java.util.Map;

/**
 * Created by domi on 17/09/16.
 */
public interface ConfigFileStore {
    public Collection<Config> getConfigs();

    public Collection<Config> getConfigs(Class<? extends Descriptor> descriptor);

    public Config getById(String id);

    public void save(Config config);

    public void remove(String id);

    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs();

    /**
     * Records that the given build used the configuration file with the given id, this file
     * being one managed by this store (i.e. this is the location of the config).
     *
     * @param configId id of the configuration file that was used
     * @param run      the build that used it
     */
    public void trackUsage(String configId, Run<?, ?> run);

    /**
     * Returns the recorded usage of the configuration file with the given id, as a map of job
     * full name to the {@link RangeSet} of build numbers that used it.
     *
     * @param configId id of the configuration file
     * @return never {@code null}, empty if the configuration file has not been recorded as used
     */
    public Map<String, RangeSet> getUsage(String configId);
}
