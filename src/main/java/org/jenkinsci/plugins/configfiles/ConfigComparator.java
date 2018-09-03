package org.jenkinsci.plugins.configfiles;

import org.jenkinsci.lib.configprovider.model.Config;

import java.io.Serializable;
import java.util.Comparator;

public class ConfigComparator implements Comparator<Config>, Serializable {
    @Override
    public int compare(Config o1, Config o2) {
        return o1.id.compareTo(o2.id);
    }
}