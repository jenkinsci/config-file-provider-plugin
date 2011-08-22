package org.jenkinsci.plugins.configfiles.groovy;

import hudson.Extension;

import org.jenkinsci.lib.configprovider.AbstractConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ConfigDescription;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;

@Extension
public class GroovyConfigProvider extends AbstractConfigProvider {

	@Override
	public ConfigDescription getConfigDescription() {
		return new ConfigDescription(Messages.groovy_provider_name(), Messages.groovy_provider_description());
	}

	@Override
	public Config newConfig() {
		String id = getProviderId() + System.currentTimeMillis();
		return new Config(id, "GroovyConfig", "", "println('hello world')");
	}

	@Override
	protected String getXmlFileName() {
		return "groovy-config-files.xml";
	}

	@Override
	public ContentType getContentType() {
		return ContentType.DefinedType.GROOVY;
	}

}
