package org.jenkinsci.plugins.configfiles;

import org.jenkinsci.lib.configprovider.model.Config;

import java.io.Serializable;
import java.util.Comparator;

public class ConfigByIdComparator implements Comparator<Config>, Serializable {

    @Override
    public int compare(Config c1, Config c2) {
        return c1.id.compareTo(c2.id);
    }
}
