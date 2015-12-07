package org.jenkinsci.lib.configprovider;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.listeners.SaveableListener;

import java.io.File;
import java.io.IOException;
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

/**
 * Partial default implementation of {@link ConfigProvider}.
 * 
 * Subtype must call the {@link #load()} method in the constructor.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractConfigProviderImpl extends ConfigProvider {

    private static final Logger LOGGER = Logger.getLogger(AbstractConfigProviderImpl.class.getName());

    protected Map<String, Config> configs = new HashMap<String, Config>();

    public AbstractConfigProviderImpl() {
    }

    @Override
    public Collection<Config> getAllConfigs() {
        List<Config> c = new ArrayList<Config>(configs.values());
        Collections.sort(c, new NameComparator());
        return Collections.unmodifiableCollection(c);
    }

    @Override
    public Config getConfigById(String configId) {
        return configs.get(configId);
    }

    @Override
    public boolean configExists(String configId) {
        return configs.containsKey(configId);
    }
    
    @Override
    public String getProviderId() {
        return getId();
    }

    @Override
    public boolean isResponsibleFor(@NonNull String configId) {
        if (this.configs.containsKey(configId)) {
            return true;
        }

        // backward compatibility - older than 2.10
        if (configId.startsWith(getProviderId())) {
            return true;
        }

        return false;
    }

    @Override
    public Config newConfig() {
        String id = this.getProviderId() + "." + System.currentTimeMillis();
        return new Config(id, null, null, null);
    }

    @Override
    public void remove(String configId) {
        configs.remove(configId);
        this.save();
    }

    @Override
    public void save(Config config) {
        configs.put(config.id, config);
        this.save();
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

    protected XmlFile getConfigXml() {
        return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getInstance().getRootDir(), this.getXmlFileName()));
    }

    protected String getXmlFileName() {
        return getId() + ".xml";
    }

    private static final class NameComparator implements Comparator<Config> {
        public int compare(Config o1, Config o2) {
            String a = o1.name != null ? o1.name : "";
            String b = o2.name != null ? o2.name : "";
            return a.compareTo(b);
        }
    }
}
