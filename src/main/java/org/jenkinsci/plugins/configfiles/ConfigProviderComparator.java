package org.jenkinsci.plugins.configfiles;

import org.jenkinsci.lib.configprovider.ConfigProvider;

import java.util.Comparator;

/**
 * Created by domi on 18/01/17.
 */
public class ConfigProviderComparator implements Comparator<ConfigProvider> {
    @Override
    public int compare(ConfigProvider c1, ConfigProvider c2) {
        return c1.getProviderId().compareTo(c2.getProviderId());
    }
}
