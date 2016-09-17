package org.jenkinsci.plugins.configfiles;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class FolderConfigFileProperty extends AbstractFolderProperty<AbstractFolder<?>> implements ConfigFileStore {

    private Collection<Config> configs = new ArrayList<Config>();

    /*package*/ FolderConfigFileProperty(AbstractFolder<?> owner) {
        setOwner(owner);
    }

    @Override
    public Collection<Config> getConfigs() {
        return configs;
    }

    @Override
    public Config getById(String id) {
        return null;
    }

    @Override
    public void save(Config config) {

    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Folder Config File Property";
        }
    }

}
