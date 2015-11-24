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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import net.sf.json.JSONObject;

/**
 * Provides a new link in the "Manage Jenkins" view and builds the UI to manage the configfiles.
 * 
 * @author domi
 * 
 */
@Extension
public class ConfigFilesManagement extends ManagementLink {

    private static final String ICON_PATH = "/plugin/config-file-provider/images/cfg_logo.png";

    public ConfigFilesManagement() {
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
     * used by index.jelly to resolve the correct path to the icon (see JENKINS-24441)
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

    public List<ConfigProvider> getProviders() {
        return ConfigProvider.all();
    }

    public Collection<Config> getConfigs() {
        List<Config> all = new ArrayList<Config>();
        for (ConfigProvider provider : ConfigProvider.all()) {
            all.addAll(provider.getAllConfigs());
        }
        return Collections.unmodifiableCollection(all);
    }

    public HttpResponse doSaveConfig(StaplerRequest req) {
        checkPermission(Hudson.ADMINISTER);
        try {
            JSONObject json = req.getSubmittedForm().getJSONObject("config");
            Config config = req.bindJSON(Config.class, json);

            for (ConfigProvider provider : ConfigProvider.all()) {
                if (provider.isResponsibleFor(config.id)) {
                    provider.save(config);
                }
            }

        } catch (ServletException e) {
            e.printStackTrace();
        }
        return new HttpRedirect("index");
    }

    public void doShow(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String confgiId) throws IOException, ServletException {

        ConfigProvider provider = getProviderForConfigId(confgiId);
        Config config = provider.getConfigById(confgiId);
        if (config != null) {
            req.setAttribute("contentType", provider.getContentType());
            req.setAttribute("config", config);
            req.getView(this, "show.jelly").forward(req, rsp);
        }
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

        ConfigProvider provider = getProviderForConfigId(confgiId);
        Config config = provider.getConfigById(confgiId);
        if (config != null) {
            req.setAttribute("contentType", provider.getContentType());
            req.setAttribute("config", config);
            req.setAttribute("provider", provider);
            req.getView(this, "edit.jelly").forward(req, rsp);
        } else {
            req.getView(this, "index").forward(req, rsp);
        }
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
    public void doAddConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("configIdSuffix") String configIdSuffix, @QueryParameter("providerId") String providerId) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);

        for (ConfigProvider provider : ConfigProvider.all()) {
            if (provider.getProviderId().equals(providerId)) {
                req.setAttribute("contentType", provider.getContentType());
                req.setAttribute("provider", provider);
                Config config = provider.newConfig(configIdSuffix);
                req.setAttribute("config", config);
                break;
            }
        }

        req.getView(this, "edit.jelly").forward(req, rsp);
    }

    public void doSelectProvider(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);
        req.setAttribute("providers", ConfigProvider.all());
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
        for (ConfigProvider provider : ConfigProvider.all()) {
            if (provider.isResponsibleFor(configId)) {
                provider.remove(configId);
            }
        }
        return new HttpRedirect("index");
    }

    private ConfigProvider getProviderForConfigId(String id) {
        if (!StringUtils.isBlank(id)) {
            for (ConfigProvider provider : ConfigProvider.all()) {
                if (provider.isResponsibleFor(id)) {
                    return provider;
                }
            }
        }
        return null;
    }
}
