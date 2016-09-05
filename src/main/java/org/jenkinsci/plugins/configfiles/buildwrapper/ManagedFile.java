/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author domi
 */
public class ManagedFile implements ExtensionPoint, Describable<ManagedFile> {

    public final String fileId;
    public final String targetLocation;
    public final String variable;
    /**
     * whether tokens in the content of the file must be replaced by the TokenMacro plugin
     *
     * @since 2.10.2
     */
    private final Boolean replaceTokens;

    @DataBoundConstructor
    public ManagedFile(String fileId, String targetLocation, String variable, Boolean replaceTokens) {
        this.fileId = fileId;
        this.targetLocation = targetLocation;
        this.variable = variable;
        this.replaceTokens = replaceTokens;
    }

    public ManagedFile(String fileId, String targetLocation, String variable) {
        this.fileId = fileId;
        this.targetLocation = targetLocation;
        this.variable = variable;
        this.replaceTokens = false;
    }

    @Override
    public String toString() {
        return "[ManagedFile: id=" + fileId + ", targetLocation=" + targetLocation + ", variable=" + variable + "]";
    }

    public Boolean getReplaceTokens() {
        return replaceTokens != null ? replaceTokens : false;
    }

    @Override
    public Descriptor<ManagedFile> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }


    @Symbol("configFile")
    @Extension
    public static class DescriptorImpl extends Descriptor<ManagedFile> {
        @Override
        public String getDisplayName() {
            return null;
        }

        public ListBoxModel doFillFileIdItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("please select", "");
            for (Config config : getConfigFiles()) {
                items.add(config.name, config.id);
            }
            return items;
        }

        public Collection<Config> getConfigFiles() {
            ExtensionList<ConfigProvider> providers = ConfigProvider.all();
            List<Config> allFiles = new ArrayList<Config>();
            for (ConfigProvider provider : providers) {
                allFiles.addAll(provider.getAllConfigs());
            }
            return allFiles;
        }
    }
}

