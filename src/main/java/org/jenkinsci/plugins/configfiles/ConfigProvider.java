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

import hudson.ExtensionList;
import hudson.ExtensionPoint;

import java.util.Collection;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ConfigDescription;
import org.jenkinsci.plugins.configfiles.model.ContentType;

public abstract class ConfigProvider implements ExtensionPoint {

	/**
	 * All registered {@link ConfigProvider}s.
	 */
	public static ExtensionList<ConfigProvider> all() {
		return Jenkins.getInstance().getExtensionList(ConfigProvider.class);
	}

	public abstract Collection<Config> getAllConfigs();

	public abstract ConfigDescription getConfigDescription();

	public abstract ContentType getContentType();

	public abstract Config getConfigById(String configId);

	public abstract boolean isResponsibleFor(String configId);

	public abstract void save(Config config);

	public abstract void remove(String configId);

	/**
	 * An ID uniquely identifying this provider, the id of each {@link Config}
	 * must start with this ID separated by a '.'!
	 * 
	 * @return the unique id for this provider.
	 */
	public abstract String getProviderId();

	/**
	 * Returns a new {@link Config} object with a unique id, starting with the
	 * id of this provider - separated by '.'. e.g. "MyCustomProvider.123456".
	 * This object is also used initialize the user interface.
	 * 
	 * @return the new config object, ready for editing.
	 */
	public abstract Config newConfig();

}
