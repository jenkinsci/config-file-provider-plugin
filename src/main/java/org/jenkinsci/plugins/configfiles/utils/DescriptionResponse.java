package org.jenkinsci.plugins.configfiles.utils;

import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Can be used instead of a {@link FormValidation#ok(String)} to give the user some information about
 * the item to checked (e.g. a selection in  a dropdown)
 */
public class DescriptionResponse implements HttpResponse {

    private String html = "div/>";

    public DescriptionResponse(String html) {
        if (StringUtils.isNotBlank(html)) {
            this.html = html;
        }
    }

    @Override
    public void generateResponse(StaplerRequest2 req, StaplerResponse2 rsp, Object node) throws IOException, ServletException {
        rsp.setContentType("text/html;charset=UTF-8");
        rsp.getWriter().print(html);
    }
}
