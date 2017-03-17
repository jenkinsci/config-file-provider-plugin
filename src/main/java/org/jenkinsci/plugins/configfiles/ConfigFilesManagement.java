/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletException;

import hudson.Extension;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Provides a new link in the "Manage Jenkins" view and builds the UI to manage the configfiles.
 * 
 * @author domi
 * 
 */
@Extension
public class ConfigFilesManagement extends ManagementLink implements ConfigFilesUIContract {

    public static final String ICON_PATH = "/plugin/config-file-provider/images/cfg_logo.png";

    private ConfigFileStore store;

    public ConfigFilesManagement() {
        this.store = GlobalConfigFiles.get();
    }

    /**
     * @see hudson.model.Action#getDisplayName()
     */
    public String getDisplayName() {
        return Messages.display_name();
    }

    @Override
    public String getDescription() {
        return Messages.description();
    }

    /**
     * @see hudson.model.ManagementLink#getIconFileName()
     */
    @Override
    public String getIconFileName() {
        return ICON_PATH;
    }

    /**
     * used by configfiles.jelly to resolve the correct path to the icon (see JENKINS-24441)
     */
    public String getIconUrl(String rootUrl) {
        if (rootUrl.endsWith("/")) {
            return rootUrl + ICON_PATH.substring(1);
        }
        return rootUrl + ICON_PATH;
    }

    /**
     * @see hudson.model.ManagementLink#getUrlName()
     */
    @Override
    public String getUrlName() {
        return "configfiles";
    }

    public ContentType getContentTypeForProvider(String providerId) {
        for (ConfigProvider provider : ConfigProvider.all()) {
            if (provider.getProviderId().equals(providerId)) {
                return provider.getContentType();
            }
        }
        return null;
    }

    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs() {
        return store.getGroupedConfigs();
    }

    public List<ConfigProvider> getProviders() {
        return ConfigProvider.all();
    }

    public Collection<Config> getConfigs() {
        return Collections.unmodifiableCollection(store.getConfigs());
    }

    /**
     * Insert or update
     * @param req
     * @return
     */
    public HttpResponse doSaveConfig(StaplerRequest req) {
        checkPermission(Hudson.ADMINISTER);
        try {
            JSONObject json = req.getSubmittedForm().getJSONObject("config");
            Config config = req.bindJSON(Config.class, json);

            // potentially replace existing
            store.save(config);

        } catch (ServletException e) {
            e.printStackTrace();
        }
        return new HttpRedirect("index");
    }

    public void doShow(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String confgiId) throws IOException, ServletException {

        Config config = store.getById(confgiId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.getView(this, "show.jelly").forward(req, rsp);

    }

    /**
     * Loads the config by its id and forwards the request to "edit.jelly".
     * 
     * @param req
     *            request
     * @param rsp
     *            response
     * @param confgiId
     *            the id of the config to be loaded in to the edit view.
     * @throws IOException
     * @throws ServletException
     */
    public void doEditConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String confgiId) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);

        Config config = store.getById(confgiId);
        req.setAttribute("contentType", config.getProvider().getContentType());
        req.setAttribute("config", config);
        req.setAttribute("provider", config.getProvider());
        req.getView(this, "edit.jelly").forward(req, rsp);
    }

    /**
     * Requests a new config object from provider (defined by the given id) and forwards the request to "edit.jelly".
     * 
     * @param req
     *            request
     * @param rsp
     *            response
     * @param providerId
     *            the id of the provider to create a new config instance with
     * @throws IOException
     * @throws ServletException
     */
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

    public void doSelectProvider(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);
        req.setAttribute("providers", ConfigProvider.all());
        req.setAttribute("configId", UUID.randomUUID().toString());
        req.getView(this, "selectprovider.jelly").forward(req, rsp);
    }

    private void checkPermission(Permission permission) {
        Hudson.getInstance().checkPermission(permission);
    }

    /**
     * Removes a script from the config and filesystem.
     * 
     * @param res
     *            response
     * @param rsp
     *            request
     * @param configId
     *            the id of the config to be removed
     * @return forward to 'index'
     * @throws IOException
     */
    public HttpResponse doRemoveConfig(StaplerRequest res, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException {
        checkPermission(Hudson.ADMINISTER);

        store.remove(configId);

        return new HttpRedirect("index");
    }

    public FormValidation doCheckConfigId(@QueryParameter("configId") String configId) {
        if (configId == null || configId.isEmpty()) {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdCannotBeEmpty());
        }

        Config config = store.getById(configId);
        if (config == null) {
            return FormValidation.ok();
        } else {
            return FormValidation.warning(Messages.ConfigFilesManagement_configIdAlreadyUsed(config.name, config.id));
        }
    }
}
