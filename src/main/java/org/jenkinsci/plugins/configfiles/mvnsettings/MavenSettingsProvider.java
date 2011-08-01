package org.jenkinsci.plugins.configfiles.mvnsettings;

import hudson.Extension;

import org.jenkinsci.plugins.configfiles.AbstractConfigProvider;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ContentType;

@Extension
public class MavenSettingsProvider extends AbstractConfigProvider {

	@Override
	public ContentType getSupportedContentType() {
		return ContentType.XML;
	}

	@Override
	public Config newConfig() {
		String id = ID_PREFIX + System.currentTimeMillis();
		return new Config(id, "MySettings", "", "<settings></settings>", "XML");
	}

	@Override
	protected String getXmlFileName() {
		return "maven-settings-files.xml";
	}
}
