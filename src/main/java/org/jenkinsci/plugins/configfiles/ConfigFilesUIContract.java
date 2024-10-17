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

import hudson.util.FormValidation;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.*;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Defines the contract for actions called by jelly
 *
 * @author domi
 *
 */
public interface ConfigFilesUIContract {

    public static final String JELLY_RESOURCES_PATH = "/org/jenkinsci/plugins/configfiles/ConfigFilesUI/";

    public static final String ICON_PATH = "symbol-cfg-logo plugin-config-file-provider";

    public static Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]*$");

    public ContentType getContentTypeForProvider(String providerId);

    public Map<ConfigProvider, Collection<Config>> getGroupedConfigs();

    public List<ConfigProvider> getProviders();

    /**
     * Insert or update
     *
     * @param req request
     * @return
     */
    public HttpResponse doSaveConfig(StaplerRequest2 req) throws IOException, ServletException ;

    public void doShow(StaplerRequest2 req, StaplerResponse2 rsp, @QueryParameter("id") String configId) throws IOException, ServletException;

    /**
     * Loads the config by its id and forwards the request to "edit.jelly".
     *
     * @param req
     *            request
     * @param rsp
     *            response
     * @param configId
     *            the id of the config to be loaded in to the edit view.
     * @throws IOException
     * @throws ServletException
     */
    public void doEditConfig(StaplerRequest2 req, StaplerResponse2 rsp, @QueryParameter("id") String configId) throws IOException, ServletException;

    /**
     * Requests a new config object from provider (defined by the given id) and forwards the request to "edit.jelly".
     *
     * @param req
     *            request
     * @param rsp
     *            response
     * @param providerId
     *            the id of the provider to create a new config instance with
     * @param configId
     *            the id of the new config instance
     * @throws IOException
     * @throws ServletException
     */
    public void doAddConfig(StaplerRequest2 req, StaplerResponse2 rsp, @QueryParameter("providerId") String providerId, @QueryParameter("configId") String configId) throws IOException, ServletException;

    public void doSelectProvider(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException;

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
    public HttpResponse doRemoveConfig(StaplerRequest2 res, StaplerResponse2 rsp, @QueryParameter("id") String configId) throws IOException;

    public FormValidation doCheckConfigId(@QueryParameter("configId") String configId);
}
