package org.jenkinsci.plugins.mvnsettings.model;

public enum ContentType {

	XML("xml", "application/xml"), HTML("htmlmixed", "text/html");

	public final String cmMode;
	public final String mime;

	private ContentType(String cmMode, String mime) {
		this.cmMode = cmMode;
		this.mime = mime;
	}

}
