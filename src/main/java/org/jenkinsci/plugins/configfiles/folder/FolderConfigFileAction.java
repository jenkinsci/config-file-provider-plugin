package org.jenkinsci.plugins.configfiles.folder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.ConfigFilesManagement;
import org.jenkinsci.plugins.configfiles.ConfigFilesUIContract;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Permission;
import hudson.util.FormValidation;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;

import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

public class FolderConfigFileAction implements Action, ConfigFilesUIContract, StaplerProxy {

    private AbstractFolder<?> folder;

    FolderConfigFileAction(AbstractFolder<?> folder) {
        this.folder = folder;
    }

    public AbstractFolder<?> getFolder() {
        return folder;
    }

    @Override
    public String getIconFileName() {
        /*
         * only show the action if you can either
         * a) add a new entry
         * b) there is an existing entry
         */
        boolean hasPerm = folder.hasPermission(Item.CONFIGURE) || (folder.hasPermission(Item.EXTENDED_READ) && hasStore());
        return hasPerm ? ConfigFilesManagement.ICON_PATH : null;
    }

    /**
     * used by configfiles.jelly to resolve the correct path to the icon (see JENKINS-24441)
     */
    @Restricted(NoExternalUse.class)
    public String getIconUrl(String rootUrl) {
        if (rootUrl.endsWith("/")) {
            return rootUrl + ConfigFilesManagement.ICON_PATH.substring(1);
        }
        return rootUrl + ConfigFilesManagement.ICON_PATH;
    }

    @Override
    public String getDisplayName() {
        return folder.hasPermission(Item.EXTENDED_READ) ? "Config Files" : null;
    }

    @Override
    public String getUrlName() {
        return folder.hasPermission(Item.EXTENDED_READ) ? "configfiles" : null;
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
        ConfigFileStore store = getStore();
        Map<ConfigProvider, Collection<Config>> groupedConfigs = store.getGroupedConfigs();
        return groupedConfigs;
    }

    @Override
    public List<ConfigProvider> getProviders() {
        List<ConfigProvider> all = ConfigProvider.all();
        List<ConfigProvider> folderSupportedProviders = new ArrayList<>();
        for (ConfigProvider p : all) {
            if (p.supportsFolder()){
                folderSupportedProviders.add(p);
            }
        }
        return folderSupportedProviders;
    }

    @Override
    @POST
    public HttpResponse doSaveConfig(StaplerRequest req) throws IOException, ServletException {
        checkPermission(Item.CONFIGURE);
        try {
            JSONObject json = req.getSubmittedForm().getJSONObject("config");
            Config config = req.bindJSON(Config.class, json);

            if(!ID_PATTERN.matcher(config.id).matches()){
                return FormValidation.error(Messages.ConfigFilesManagement_configIdContainsInvalidCharacters());
            }

            ConfigFileStore store = getStore();
            // potentially replace existing
            store.save(config);

        } catch (ServletException e) {
            e.printStackTrace();
        }
        return new HttpRedirect("index");
    }

    private boolean hasStore() {
        return folder.getProperties().get(FolderConfigFileProperty.class) != null;
    }

    ConfigFileStore getStore() {
        // TODO only add property when its really needed (eg. don't add it if there is no config to be saved)
        FolderConfigFileProperty folderConfigFileProperty = folder.getProperties().get(FolderConfigFileProperty.class);
        if(folderConfigFileProperty == null) {
            folderConfigFileProperty = new FolderConfigFileProperty();
            try {
                folder.addProperty(folderConfigFileProperty);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return folderConfigFileProperty;
    }

    @Override
    public void doShow(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException, ServletException {
        folder.checkPermission(Item.EXTENDED_READ);
        Config config = getStore().getById(configId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.getView(this, JELLY_RESOURCES_PATH + "show.jelly").forward(req, rsp);
    }

    @Override
    public void doEditConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException, ServletException {
        checkPermission(Job.CONFIGURE);
        Config config = getStore().getById(configId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.setAttribute("provider", config.getProvider());
        req.getView(this, JELLY_RESOURCES_PATH + "edit.jelly").forward(req, rsp);
    }


    @Override
    @POST
    public void doAddConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("providerId") String providerId, @QueryParameter("configId") String configId) throws IOException, ServletException {
        checkPermission(Item.CONFIGURE);

        FormValidation error = null;
        if (providerId == null || providerId.isEmpty()) {
            error = FormValidation.errorWithMarkup(Messages._ConfigFilesManagement_selectTypeOfFileToCreate().toString(req.getLocale()));
        }
        if (configId == null || configId.isEmpty()) {
            error = FormValidation.errorWithMarkup(Messages._ConfigFilesManagement_configIdCannotBeEmpty().toString(req.getLocale()));
        }
        if (!ID_PATTERN.matcher(configId).matches()) {
            error = FormValidation.error(Messages.ConfigFilesManagement_configIdContainsInvalidCharacters());
        }

        if (error != null) {
            req.setAttribute("error", error);
            checkPermission(Job.CONFIGURE);
            req.setAttribute("providers", getProviders());
            req.setAttribute("configId", configId);
            req.getView(this, JELLY_RESOURCES_PATH + "selectprovider.jelly").forward(req, rsp);
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

        req.getView(this, JELLY_RESOURCES_PATH + "edit.jelly").forward(req, rsp);
    }

    @Override
    public void doSelectProvider(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(Job.CONFIGURE);
        req.setAttribute("providers", getProviders());
        req.setAttribute("configId", UUID.randomUUID().toString());
        req.getView(this, JELLY_RESOURCES_PATH + "selectprovider.jelly").forward(req, rsp);
    }

    @RequirePOST
    @Override
    public HttpResponse doRemoveConfig(StaplerRequest res, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException {
        checkPermission(Job.CONFIGURE);

        getStore().remove(configId);

        return new HttpRedirect("index");
    }

    @Override
    public FormValidation doCheckConfigId(@QueryParameter("configId") String configId) {
        checkPermission(Job.CONFIGURE);
        
        if (configId == null || configId.isEmpty()) {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdCannotBeEmpty());
        }

        if(!ID_PATTERN.matcher(configId).matches()){
            return FormValidation.error(Messages.ConfigFilesManagement_configIdContainsInvalidCharacters());
        }

        Config config = getStore().getById(configId);
        if (config == null) {
            return FormValidation.ok();
        } else {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdAlreadyUsed(config.name, config.id));
        }
    }

    @Extension(optional = true)
    @SuppressWarnings("rawtypes")
    public static class ActionFactory extends TransientActionFactory<AbstractFolder> {
        @Override
        public Class<AbstractFolder> type() {
            return AbstractFolder.class;
        }

        @Override
        public Collection<? extends Action> createFor(AbstractFolder target) {
            return Collections.singleton(new FolderConfigFileAction(target));
        }
    }

    private void checkPermission(Permission permission) {
        folder.checkPermission(permission);
    }

    @Override
    public Object getTarget() {
        checkPermission(Item.EXTENDED_READ);
        return this;
    }
}
