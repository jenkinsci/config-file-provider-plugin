package org.jenkinsci.plugins.configfiles.utils;

import hudson.model.Item;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest2;

public class ConfigFileDetailLinkDescription extends DescriptionResponse {
    private ConfigFileDetailLinkDescription(String linkHtml) {
        super(linkHtml);
    }

    public static ConfigFileDetailLinkDescription getDescription(StaplerRequest2 req, Item context, String fileId) {
        return new ConfigFileDetailLinkDescription(getDetailsLink(req, context, fileId));
    }

    private static String getDetailsLink(StaplerRequest2 req, Item context, String fileId) {
        String link = req.getContextPath();
        link = StringUtils.isNotBlank(context.getUrl()) ? link + "/" + context.getUrl() : link;
        link = link + "configfiles/show?id=" + fileId;
        String linkHtml = "<a target=\"_blank\" href=\"" + link + "\">view selected file</a>";

        // 1x16 spacer needed for IE since it doesn't support min-height
        return "<div class='ok'><img src='" +
                req.getContextPath() + Jenkins.RESOURCE_PATH + "/images/none.gif' height=16 width=1>" +
                linkHtml + "</div>";

    }

}
