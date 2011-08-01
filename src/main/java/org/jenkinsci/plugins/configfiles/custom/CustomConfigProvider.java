package org.jenkinsci.plugins.configfiles.custom;

import hudson.Extension;

import org.jenkinsci.plugins.configfiles.AbstractConfigProvider;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ConfigDescription;
import org.jenkinsci.plugins.configfiles.model.ContentType;

@Extension
public class CustomConfigProvider extends AbstractConfigProvider {

	@Override
	public ConfigDescription getConfigDescription() {
		return new ConfigDescription(Messages.custom_provider_name(), Messages.custom_provider_description());
	}

	@Override
	public Config newConfig() {
		String id = getProviderId() + System.currentTimeMillis();
		return new Config(id, "MyCustom", "", "");
	}

	@Override
	protected String getXmlFileName() {
		return "custom-config-files.xml";
	}

	@Override
	public ContentType getContentType() {
		return null;
	}

}
