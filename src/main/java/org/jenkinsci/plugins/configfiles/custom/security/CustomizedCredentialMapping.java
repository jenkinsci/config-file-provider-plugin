/*
 The MIT License

 Copyright (c) 2020, Andrew Grimberg

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
package org.jenkinsci.plugins.configfiles.custom.security;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;
import java.util.Collections;

public class CustomizedCredentialMapping extends AbstractDescribableImpl<CustomizedCredentialMapping> implements Serializable {

    private final String tokenKey;
    private final String credentialsId;

    @DataBoundConstructor
    public CustomizedCredentialMapping(String tokenKey, String credentialsId) {
        this.tokenKey = tokenKey;
        this.credentialsId = credentialsId;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CustomizedCredentialMapping> {

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId
                ) {

            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
                return result
                        .includeMatchingAs(
                                ACL.SYSTEM,
                                Jenkins.get(),
                                StandardUsernameCredentials.class,
                                Collections.emptyList(),
                                CredentialsMatchers.instanceOf(
                                        StandardUsernameCredentials.class))
                        .includeMatchingAs(
                                ACL.SYSTEM,
                                Jenkins.get(),
                                StringCredentials.class,
                                Collections.emptyList(),
                                CredentialsMatchers.instanceOf(
                                        StringCredentials.class))
                        .includeCurrentValue(credentialsId);
            }

            if (!item.hasPermission(Item.EXTENDED_READ)
                && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
            return result
                    .includeMatchingAs(
                            item instanceof Queue.Task
                                    ? Tasks.getAuthenticationOf((Queue.Task) item)
                                    : ACL.SYSTEM,
                            item,
                            StandardUsernameCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(
                                    StandardUsernameCredentials.class))
                    .includeMatchingAs(
                            item instanceof Queue.Task
                                    ? Tasks.getAuthenticationOf((Queue.Task) item)
                                    : ACL.SYSTEM,
                            item,
                            StringCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(
                                    StringCredentials.class))
                    .includeCurrentValue(credentialsId);

        }
    }
}
