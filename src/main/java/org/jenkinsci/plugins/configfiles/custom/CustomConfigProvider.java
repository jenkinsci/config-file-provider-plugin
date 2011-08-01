package org.jenkinsci.plugins.configfiles.custom;

import hudson.Extension;

import org.jenkinsci.plugins.configfiles.AbstractConfigProvider;
import org.jenkinsci.plugins.configfiles.model.Config;
import org.jenkinsci.plugins.configfiles.model.ContentType;

@Extension
public class CustomConfigProvider extends AbstractConfigProvider {

	@Override
	public ContentType getSupportedContentType() {
		return ContentType.HTML;
	}

	@Override
	public Config newConfig() {
		String id = ID_PREFIX + System.currentTimeMillis();
		return new Config(id, "MySettings", "", "<html></html>", ContentType.HTML.name());
	}

	@Override
	protected String getXmlFileName() {
		return "custom-config-files.xml";
	}

}
