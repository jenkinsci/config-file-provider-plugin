package org.jenkinsci.lib.configprovider;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.listeners.SaveableListener;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;

/**
 * Partial default implementation of {@link ConfigProvider}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractConfigProviderImpl extends ConfigProvider {

    private static final Logger LOGGER = Logger.getLogger(AbstractConfigProviderImpl.class.getName());

    @Deprecated
    protected Map<String, Config> configs = new HashMap<String, Config>();

    public AbstractConfigProviderImpl() {
    }

    /**
     * only used for data migration
     *
     * @see org.jenkinsci.plugins.configfiles.ConfigFiles
     */
    @Deprecated
    public Map<String, Config> getConfigs() {

        Map<String, Config> tmp = new HashMap<String, Config>();
        for (Map.Entry<String, Config> c : configs.entrySet()) {
            // many provider implementations saved there config objects with the base Config type instead of the concrete type
            tmp.put(c.getKey(), convert(c.getValue()));
        }

        return tmp;
    }

    /**
     * Only used to convert data from the old (&lt; 1.5) to the new (&ge; 1.5) storage format.
     * New implementations of this extension point do not need to implement this.
     *
     * @param config the configuration to convert
     * @param <T> expected type of the returned configuration item.
     */
    @Deprecated
    public <T extends Config> T convert(Config config) {
        Config convertedConfig = this.newConfig(config.id, config.name, config.comment, config.content);
        config.setProviderId(config.getProviderId());
        return (T) convertedConfig;
    }


    @Override
    public String getProviderId() {
        return getId();
    }


    /**
     * Saves the configuration info to the disk.
     */
    public synchronized void save() {
        if (BulkChange.contains(this))
            return;
        try {
            getConfigXml().write(this);
            SaveableListener.fireOnChange(this, getConfigXml());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save " + getConfigXml(), e);
        }
    }

    /**
     * Overridden for backward compatibility to let subtype customize the file name.
     */
    public void load() {
        XmlFile xml = getConfigXml();
        if (xml.exists()) {
            try {
                xml.unmarshal(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Deprecated
    protected XmlFile getConfigXml() {
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.get().getRootDir(), this.getXmlFileName()));
    }

    static {
        Jenkins.XSTREAM.alias("org.jenkinsci.lib.configprovider.model.Config", CustomConfig.class);
    }

    @Deprecated
    protected String getXmlFileName() {
        return getId() + ".xml";
    }

    public void clearOldDataStorage() {
        if (configs != null && !configs.isEmpty()) {
            configs = Collections.emptyMap();
            File file = getConfigXml().getFile();
            if (!file.delete()) {
                LOGGER.info("Unable to delete " + file.getAbsolutePath());
            }
        }
    }
}
