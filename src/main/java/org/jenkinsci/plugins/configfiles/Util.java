package org.jenkinsci.plugins.configfiles;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;

/**
 * Util to work with {@link ConfigProvider}s
 * 
 * @author Dominik Bartholdi (imod)
 */
public class Util {

    private Util() {
    }

    public static <T extends ConfigProvider> T getProviderForConfigIdOrNull(String id) {
        if (!StringUtils.isBlank(id)) {
            for (ConfigProvider provider : ConfigProvider.all()) {
                if (provider.isResponsibleFor(id)) {
                    return (T) provider;
                }
            }
        }
        return null;
    }
}
