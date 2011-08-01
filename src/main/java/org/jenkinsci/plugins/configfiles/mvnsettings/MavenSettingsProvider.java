package org.jenkinsci.plugins.configfiles.mvnsettings;

import hudson.Extension;

import org.jenkinsci.plugins.configfiles.AbstractConfigProvider;
import org.jenkinsci.plugins.configfiles.Messages;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ConfigDescription;
import org.jenkinsci.plugins.configfiles.model.ContentType;
import org.jenkinsci.plugins.configfiles.model.ContentType.DefinedType;

@Extension
public class MavenSettingsProvider extends AbstractConfigProvider {

	@Override
	public ConfigDescription getConfigDescription() {
		return new ConfigDescription(Messages.mvn_settings_provider_name(), Messages.mvn_settings_provider_description());
	}

	@Override
	public Config newConfig() {
		String id = this.getProviderId() + System.currentTimeMillis();
		return new Config(id, "MySettings", "", "<settings></settings>");
	}

	@Override
	protected String getXmlFileName() {
		return "maven-settings-files.xml";
	}

	@Override
	public ContentType getContentType() {
		return DefinedType.HTML;
	}
}
