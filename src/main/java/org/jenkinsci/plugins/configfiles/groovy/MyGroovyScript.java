package org.jenkinsci.plugins.configfiles.groovy;

import hudson.Extension;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.lib.configprovider.model.ContentType.DefinedType;

public class MyGroovyScript extends Config {
    private static final long serialVersionUID = 1L;

    public MyGroovyScript(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }

    @Extension
    public static class DescriptorImpl extends AbstractConfigProviderImpl {

        public DescriptorImpl() {
             load();
        }

        @Override
        public ContentType getContentType() {
            return DefinedType.GROOVY;
        }

        @Override
        public String getDisplayName() {
            return "Scriptler Groovy";
        }

    }

}
