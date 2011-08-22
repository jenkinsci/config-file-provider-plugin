package org.jenkinsci.plugins.configfiles.xml;

import hudson.Extension;

import org.jenkinsci.lib.configprovider.AbstractConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigDescription;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

@Extension
public class XmlConfigProvider extends AbstractConfigProvider {

	@Override
	public ConfigDescription getConfigDescription() {
		return new ConfigDescription(Messages.xml_provider_name(), Messages.xml_provider_description());
	}

	@Override
	public Config newConfig() {
		String id = getProviderId() + System.currentTimeMillis();
		return new Config(id, "XmlConfig", "", "<root></root>");
	}

	@Override
	protected String getXmlFileName() {
		return "xml-config-files.xml";
	}

	@Override
	public ContentType getContentType() {
		return ContentType.DefinedType.XML;
	}

}
