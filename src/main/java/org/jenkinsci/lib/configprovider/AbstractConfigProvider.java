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
package org.jenkinsci.lib.configprovider;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigDescription;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Backward compatibility layer for old subtypes of {@link ConfigProvider}
 * 
 * @deprecated as of 1.2.
 *      Extend {@link AbstractConfigProviderImpl} directly.
 */
public abstract class AbstractConfigProvider extends AbstractConfigProviderImpl {

	protected final String ID_PREFIX = this.getClass().getSimpleName() + ".";

	public AbstractConfigProvider() {
		load();
	}

    @Override
    public String getProviderId() {
        return ID_PREFIX;
    }

    // backward compatibility
    @Override
    public String getDisplayName() {
        return getConfigDescription().getName();
    }

    /**
     * Overridden for backward compatibility to let subtype customize the file name.
	 */
    @Override
	public void save() {
		if (BulkChange.contains(this))
			return;
		try {
			getConfigXml().write(this);
			SaveableListener.fireOnChange(this, getConfigXml());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    /**
     * Overridden for backward compatibility to let subtype customize the file name.
	 */
	public void load() {
		XmlFile xml = getConfigXml();
		if (xml.exists()) {
			try {
				xml.unmarshal(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected XmlFile getConfigXml() {
		return new XmlFile(Jenkins.XSTREAM, new File(Jenkins.getInstance().getRootDir(), this.getXmlFileName()));
	}

	protected String getXmlFileName() {
        return getClass().getName()+".xml";
    }
}
