package org.jenkinsci.plugins.configfiles;

import hudson.model.Action;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import org.jenkinsci.lib.configprovider.model.Config;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

public class ConfigFilesAction implements Action, StaplerProxy {
    private Job item;
    public ConfigFilesAction(Job item) {
        this.item = item;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "configfiles";
    }

    @Override
    public Object getTarget() {
        item.checkPermission(Item.EXTENDED_READ);
        return this;
    }

    public void doShow(StaplerRequest req, StaplerResponse rsp, @QueryParameter("id") String fileId, @AncestorInPath ItemGroup group) throws IOException, ServletException {
        List<Config> configs = ConfigFiles.getConfigsInContext(group, null);
        for (Config config : configs) {
            if (config.id.equals(fileId)) {
                req.setAttribute("contentType", config.getProvider().getContentType());
                req.setAttribute("config", config);
                req.getView(this, JELLY_RESOURCES_PATH + "show.jelly").forward(req, rsp);
                break;
            }
        }
        throw HttpResponses.notFound();
    }

    public static final String JELLY_RESOURCES_PATH = "/org/jenkinsci/plugins/configfiles/ConfigFilesUI/";

}
