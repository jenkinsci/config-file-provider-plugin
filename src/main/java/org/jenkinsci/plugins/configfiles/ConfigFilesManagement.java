/**
 * 
 */
package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.Hudson;
import hudson.security.Permission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ConfigDescription;
import org.jenkinsci.plugins.configfiles.model.ContentType;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author domi
 * 
 */
@Extension
public class ConfigFilesManagement extends ManagementLink {

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
		return "/plugin/mvn-settings/images/mvn_s.png";
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

	public Collection<Config> getConfigs() {
		List<Config> all = new ArrayList<Config>();
		for (ConfigProvider provider : ConfigProvider.all()) {
			all.addAll(provider.getAllConfigs());
		}
		return Collections.unmodifiableCollection(all);
	}

	public Set<ConfigDescription> getAllModes() {
		Set<ConfigDescription> all = new HashSet<ConfigDescription>();
		for (ConfigProvider provider : ConfigProvider.all()) {
			all.add(provider.getConfigDescription());
		}
		return Collections.unmodifiableSet(all);
	}

	public HttpResponse doSaveConfig(StaplerRequest req) {

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
			req.getView(this, "edit.jelly").forward(req, rsp);
		} else {
			req.getView(this, "index").forward(req, rsp);
		}
	}

	/**
	 * Loads the config by its id and forwards the request to "edit.jelly".
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
	public void doAddConfig(StaplerRequest req, StaplerResponse rsp, @QueryParameter("providerId") String providerId) throws IOException, ServletException {
		checkPermission(Hudson.ADMINISTER);

		Config config = null;
		for (ConfigProvider provider : ConfigProvider.all()) {
			if (provider.getProviderId().equals(providerId)) {
				req.setAttribute("contentType", provider.getContentType());
				config = provider.newConfig();
				break;
			}
		}

		req.setAttribute("config", config);
		req.getView(this, "edit.jelly").forward(req, rsp);
	}

	public void doSelectProvider(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
		checkPermission(Hudson.ADMINISTER);

		Map<String, ConfigDescription> ctypes = new HashMap<String, ConfigDescription>();
		for (ConfigProvider provider : ConfigProvider.all()) {
			ctypes.put(provider.getProviderId(), provider.getConfigDescription());
		}

		req.setAttribute("configdescriptions", ctypes);
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
