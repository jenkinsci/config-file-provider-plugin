package org.jenkinsci.plugins.configfiles.model;

public interface ContentType {

	public String getCmMode();

	public String getMime();

	public enum DefinedType implements ContentType {
		XML("xml", "application/xml"), HTML("htmlmixed", "text/html"), CUSTOM(null, null);

		public final String cmMode;
		public final String mime;

		private DefinedType(String cmMode, String mime) {
			this.cmMode = cmMode;
			this.mime = mime;
		}

		public String getCmMode() {
			return cmMode;
		}

		public String getMime() {
			return mime;
		}
	}

}
