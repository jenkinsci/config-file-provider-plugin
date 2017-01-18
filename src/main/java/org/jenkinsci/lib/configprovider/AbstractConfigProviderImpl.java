package org.jenkinsci.lib.configprovider;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.ItemGroup;
import hudson.model.listeners.SaveableListener;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.json.JsonConfig;

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
     * Only used to convert data from the old (< 1.5) storage format to the new >= 1.5.
     * New implementations of this extension point do not need to implement this
     */
    @Deprecated
    public <T extends Config> T convert(Config config) {
        return (T) config;
    }


    @Override
    public String getProviderId() {
        return getId();
    }

    @Override
    @Deprecated // use org.jenkinsci.lib.configprovider.ConfigProvider.newConfig(java.lang.String)
    public Config newConfig() {
        String id = this.getProviderId() + "." + System.currentTimeMillis();
        return new Config(id, null, null, null);
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
    @Deprecated
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
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getActiveInstance().getRootDir(), this.getXmlFileName()));
    }

    @Deprecated
    protected String getXmlFileName() {
        return getId() + ".xml";
    }

    private static final class NameComparator implements Comparator<Config>, Serializable {
        private static final long serialVersionUID = -1L;
        public int compare(Config o1, Config o2) {
            String a = o1.name != null ? o1.name : "";
            String b = o2.name != null ? o2.name : "";
            return a.compareTo(b);
        }
    }

    public void clearOldDataStorage() {
        if(configs != null && !configs.isEmpty()) {
            configs = Collections.emptyMap();
            File file = getConfigXml().getFile();
            if (!file.delete()) {
                LOGGER.info("Unable to delete " + file.getAbsolutePath());
            }
        }
    }
}
