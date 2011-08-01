/**
 * 
 */
package org.jenkinsci.plugins.configfiles;

import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.listeners.SaveableListener;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.configfiles.model.Config;
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
public class CopyOfConfigFilesManagement extends ManagementLink implements Saveable {

	private Map<String, Config> configs = new HashMap<String, Config>();

	/*
	 * private static final List<Config> configs = new ArrayList<Config>();
	 * static { configs.add(getC("first", ContentType.XML));
	 * configs.add(getC("first2", ContentType.HTML)); configs.add(getC("first4",
	 * ContentType.XML)); configs.add(getC("first5", ContentType.HTML)); }
	 */

	/**
	 * 
	 */
	public CopyOfConfigFilesManagement() {
		try {
			this.load();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see hudson.model.Action#getDisplayName()
	 */
	public String getDisplayName() {
		return "Configuration Files";
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

	@Override
	public String getDescription() {
		return "manage different maven settings";
	}

	public Collection<Config> getConfigs() {
		return Collections.unmodifiableCollection(configs.values());
	}

	private static Config getC(String name, ContentType ct) {
		return new Config(null, name, "comment " + name, "<first e='r'>content0</first>", ct.name());
	}

	public ContentType[] getAllModes() {
		return ContentType.values();
	}

	public HttpResponse doSaveConfig(StaplerRequest req) {

		try {

			JSONObject json = req.getSubmittedForm().getJSONObject("config");
			Config config = req.bindJSON(Config.class, json);
			System.out.println("++-->" + config);
			configs.put(config.id, config);

		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			System.out.println("ConfigFilesManagement.doSaveConfig()");
			Jenkins.getInstance().save();
			this.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		Config config = configs.get(confgiId);
		if (config != null) {
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
	 * @param confgiId
	 *            the id of the config to be loaded in to the edit view.
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doAddConfig(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
		checkPermission(Hudson.ADMINISTER);

		Config config = new Config(null, "MySettings", "", "<settings></settings>", null);
		req.setAttribute("config", config);
		req.getView(this, "edit.jelly").forward(req, rsp);
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
	 *            the id of the file to be removed
	 * @return forward to 'index'
	 * @throws IOException
	 */
	public HttpResponse doRemoveConfig(StaplerRequest res, StaplerResponse rsp, @QueryParameter("id") String configId) throws IOException {
		checkPermission(Hudson.ADMINISTER);

		// TODO implement remove
		return new HttpRedirect("index");
	}

	/**
	 * @see hudson.model.Saveable#save()
	 */
	public void save() throws IOException {
		System.out.println("ConfigFilesManagement.save()");
		if (BulkChange.contains(this))
			return;
		getConfigXml().write(this);
		SaveableListener.fireOnChange(this, getConfigXml());
	}

	protected void load() throws IOException {
		XmlFile xml = getConfigXml();
		if (xml.exists()) {
			xml.unmarshal(this);
		}
	}

	protected XmlFile getConfigXml() {
		return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getInstance().getRootDir(), "managed-config-files.xml"));
	}

}
