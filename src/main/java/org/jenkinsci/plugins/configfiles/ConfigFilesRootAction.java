package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.model.RootAction;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.GET;

@Extension
public class ConfigFilesRootAction implements RootAction {
    @Override
    public String getIconFileName() {
        if (hasPermission()) {
            return ConfigFilesManagement.ICON_PATH;
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return Messages.display_name();
    }

    @Override
    public String getUrlName() {
        if (hasPermission()) {
            return "configFiles";
        }
        return null;
    }


    @GET
    public void doIndex(StaplerResponse2 rsp) throws IOException {
        Jenkins.get().checkPermission(ConfigFilesManagement.MANAGE_FILES);
        rsp.sendRedirect2("../configfiles");

    }

    private boolean hasPermission() {
        Jenkins j = Jenkins.get();
        return j.hasPermission(ConfigFilesManagement.MANAGE_FILES) && !j.hasPermission(Jenkins.MANAGE);
    }
}
