package org.jenkinsci.plugins.configfiles.folder;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.security.Permission;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.ConfigFilesUIContract;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FolderConfigFileAction implements Action, ConfigFilesUIContract {

    private Folder folder;

    FolderConfigFileAction(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    public String getIconFileName() {
        return ConfigFilesManagement.ICON_PATH;
    }

    @Override
    public String getDisplayName() {
        return "Config Files";
    }

    @Override
    public String getUrlName() {
        return "configfiles";
    }

    @Override
    public ContentType getContentTypeForProvider(String providerId) {
        for (ConfigProvider provider : ConfigProvider.all()) {
            if (provider.getProviderId().equals(providerId)) {
                return provider.getContentType();
            }
        }
        return null;
    }

    @Override
    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs() {
        ConfigFileStore store = getStore(folder);
        Map<ConfigProvider, Collection<Config>> groupedConfigs = store.getGroupedConfigs();
        return groupedConfigs;
    }

    @Override
    public List<ConfigProvider> getProviders() {
        return ConfigProvider.all();
    }

    @Override
    public Collection<Config> getConfigs() {
        return null;
    }

    @Override
    public HttpResponse doSaveConfig(StaplerRequest req) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);

        try {
            JSONObject json = req.getSubmittedForm().getJSONObject("config");
            Config config = req.bindJSON(Config.class, json);

            ConfigFileStore store = getStore(folder);
            // potentially replace existing
            store.remove(config.id);
            store.save(config);

        } catch (ServletException e) {
            e.printStackTrace();
        }
        return new HttpRedirect("index");
    }

    private ConfigFileStore getStore(Folder folder) {
        // TODO only add property when its really needed (eg. don't add it if there is no config to be saved)
        FolderConfigFileProperty folderConfigFileProperty = folder.getProperties().get(FolderConfigFileProperty.class);
        if(folderConfigFileProperty == null) {
            folderConfigFileProperty = new FolderConfigFileProperty(folder);
            try {
                folder.addProperty(folderConfigFileProperty);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return folderConfigFileProperty;
    }

    @Override
    public void doShow(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String confgiId) throws IOException, ServletException {

        Config config = getStore(folder).getById(confgiId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.getView(this, "show.jelly").forward(req, rsp);

    }

    @Override
    public void doEditConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String confgiId) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);

        Config config = getStore(folder).getById(confgiId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.setAttribute("provider", config.getProvider());
        req.getView(this, "edit.jelly").forward(req, rsp);
    }

    @Override
    public void doAddConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("providerId") String providerId, @QueryParameter("configId") String configId) throws IOException, ServletException {

        checkPermission(Hudson.ADMINISTER);

        FormValidation error = null;
        if (providerId == null || providerId.isEmpty()) {
            error = FormValidation.errorWithMarkup(Messages._ConfigFilesManagement_selectTypeOfFileToCreate().toString(req.getLocale()));
        }
        if (configId == null || configId.isEmpty()) {
            error = FormValidation.errorWithMarkup(Messages._ConfigFilesManagement_configIdCannotBeEmpty().toString(req.getLocale()));
        }
        if (error != null) {
            req.setAttribute("error", error);
            checkPermission(Hudson.ADMINISTER);
            req.setAttribute("providers", ConfigProvider.all());
            req.setAttribute("configId", configId);
            req.getView(this, "selectprovider.jelly").forward(req, rsp);
            return;
        }

        ConfigProvider provider = ConfigProvider.getByIdOrNull(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for id '" + providerId + "'");
        }
        req.setAttribute("contentType", provider.getContentType());
        req.setAttribute("provider", provider);
        Config config;
        if (Util.isOverridden(ConfigProvider.class, provider.getClass(), "newConfig", String.class)) {
            config = provider.newConfig(configId);
        } else {
            config = provider.newConfig();
        }

        config.setProviderId(provider.getProviderId());
        req.setAttribute("config", config);

        req.getView(this, "edit.jelly").forward(req, rsp);
    }

    @Override
    public void doSelectProvider(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);
        req.setAttribute("providers", ConfigProvider.all());
        req.setAttribute("configId", UUID.randomUUID().toString());
        req.getView(this, "selectprovider.jelly").forward(req, rsp);
    }

    @Override
    public HttpResponse doRemoveConfig(StaplerRequest res, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException {
        checkPermission(Hudson.ADMINISTER);

        getStore(folder).remove(configId);

        return new HttpRedirect("index");
    }

    @Override
    public FormValidation doCheckConfigId(@QueryParameter("configId") String configId) {
        if (configId == null || configId.isEmpty()) {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdCannotBeEmpty());
        }

        Config config = getStore(folder).getById(configId);
        if (config == null) {
            return FormValidation.ok();
        } else {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdAlreadyUsed(config.name, config.id));
        }
    }

    @Extension
    public static class ActionFactory extends TransientActionFactory<Folder> {
        @Override
        public Class<Folder> type() {
            return Folder.class;
        }

        @Override
        public Collection<? extends Action> createFor(Folder target) {
            return Collections.singleton(new FolderConfigFileAction(target));
        }
    }

    private void checkPermission(Permission permission) {
        Hudson.getInstance().checkPermission(permission);
    }

}
