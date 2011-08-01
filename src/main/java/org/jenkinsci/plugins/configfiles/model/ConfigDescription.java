package org.jenkinsci.plugins.configfiles.model;

public class ConfigDescription {

	private final String name;
	private final String description;

	public ConfigDescription(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

}
