/*
 * Copyright 20011 Olivier Lamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.configfiles.maven;

import hudson.Extension;

import org.jenkinsci.lib.configprovider.maven.GlobalMavenSettingsProvider;
import org.jenkinsci.lib.configprovider.model.ConfigDescription;

/**
 * @author Olivier Lamy
 * @author domi (imod)
 */
@Extension
public class DefaultGlobalMavenSettingsProvider extends AbstractMavenSettingsProvider implements GlobalMavenSettingsProvider{

	@Override
	protected String getXmlFileName() {
		return "maven-global-settings-files.xml";
	}

	@Override
	public ConfigDescription getConfigDescription() {
		return new ConfigDescription(Messages.mvn_global_settings_provider_name(), Messages.mvn_global_settings_provider_description());
	}
	
	@Override
	public String getDisplayName() {
	    return Messages.mvn_global_settings_provider_name();
	}
}
