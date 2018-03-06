package org.jenkinsci.plugins.configfiles;

import org.jenkinsci.lib.configprovider.model.Config;

import java.io.Serializable;
import java.util.Comparator;

public class ConfigByNameComparator implements Comparator<Config>, Serializable {
    public static final ConfigByNameComparator INSTANCE = new ConfigByNameComparator();

    private ConfigByNameComparator() {
    }

    @Override
    public int compare(Config c1, Config c2) {
        if (c1.name != null && c2.name != null) {
            return c1.name.compareToIgnoreCase(c2.name);
        }
        if (c1.id != null) {
            return c1.id.compareTo(c2.id);
        }
        return -1;
    }
}
