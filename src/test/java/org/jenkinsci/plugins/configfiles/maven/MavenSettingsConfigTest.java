/*
 * The MIT License
 *
 * Copyright 2017-2023 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles.maven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.ConfigFileBuildWrapper;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MavenSettingsConfigTest {

    private JenkinsRule r;

    private UsernamePasswordCredentialsImpl createCredential(String id, String username, String password)
            throws Descriptor.FormException {
        UsernamePasswordCredentialsImpl credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, "", username, password);
        credentials.setUsernameSecret(true);
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        return credentials;
    }

    @BeforeEach
    void before(JenkinsRule r) throws Exception {
        this.r = r;
        GlobalConfigFiles.get()
                .save(new MavenSettingsConfig(
                        "m2settings",
                        "m2settings",
                        "",
                        "<settings/>",
                        true,
                        Collections.singletonList(new ServerCredentialMapping(
                                "myserver",
                                createCredential("creds", "bot-user-name", "bot-user-s3cr3t")
                                        .getId()))));
        GlobalConfigFiles.get()
                .save(new GlobalMavenSettingsConfig(
                        "m2GlobalSettings",
                        "m2GlobalSettings",
                        "",
                        "<settings/>",
                        true,
                        Collections.singletonList(new ServerCredentialMapping(
                                "myGlobalServer",
                                createCredential("creds2", "admin", "sensitive").getId()))));
    }

    @Test
    @Issue("SECURITY-3090")
    void withCredentials() throws Exception {
        // Smokes:
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                String.join(
                        "\n",
                        "node () {",
                        "  configFileProvider([configFile(fileId: 'm2settings', variable: 'SETTINGS'), configFile(fileId: 'm2GlobalSettings', variable: 'GOBAL_SETTINGS')]) {",
                        "    String settings = readFile(env.SETTINGS)",
                        "    echo settings",
                        "    if (!settings.equals('<settings/>')) {", // Build #2 won't have credentials to assert on
                        "      assert settings.contains('<password>bot-user-s3cr3t</password>')",
                        "      assert settings.contains('<username>bot-user-name</username>')",
                        "    }",
                        "    settings = readFile(env.GOBAL_SETTINGS)",
                        "    echo settings",
                        "    if (!settings.equals('<settings/>')) {", // Build #2 won't have credentials to assert on
                        "      assert settings.contains('<password>sensitive</password>')",
                        "      assert settings.contains('<username>admin</username>')",
                        "    }",
                        "  }",
                        "}"),
                true));
        WorkflowRun run = r.buildAndAssertSuccess(p);
        r.assertLogNotContains("<password>bot-user-s3cr3t</password>", run);
        r.assertLogNotContains("<username>bot-user-name</username>", run);
        r.assertLogNotContains("<password>sensitive</password>", run);
        r.assertLogNotContains("<username>admin</username>", run);
        r.assertLogContains("<password>****</password>", run);
        r.assertLogContains("<username>****</username>", run);
        // Missing credentials. Currently treated as nonfatal:
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .set(
                        0,
                        new UsernamePasswordCredentialsImpl(
                                CredentialsScope.SYSTEM, "creds", "", "bot-user-name", "bot-user-s3cr3t"));
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .set(
                        1,
                        new UsernamePasswordCredentialsImpl(
                                CredentialsScope.SYSTEM, "creds2", "", "admin", "sensitive"));
        WorkflowRun b2 = r.buildAndAssertSuccess(p);
        r.assertLogContains("Could not find credentials [creds] for p #2", b2);
        r.assertLogNotContains("<password>", b2);
    }

    @Test
    @Issue("SECURITY-3090")
    void freestyleWithCredentials() throws Exception {
        DumbSlave slave = r.createOnlineSlave();

        FreeStyleProject p = r.createFreeStyleProject();
        p.getBuildWrappersList()
                .add(new ConfigFileBuildWrapper(List.of(
                        new ManagedFile("m2settings", null, "SETTINGS"),
                        new ManagedFile("m2GlobalSettings", null, "GLOBAL_SETTINGS"))));
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                String settings = build.getWorkspace()
                        .child(build.getEnvironment(listener).get("SETTINGS"))
                        .readToString();
                listener.getLogger().println(settings);
                assertThat(settings, containsString("<password>bot-user-s3cr3t</password>"));
                assertThat(settings, containsString("<username>bot-user-name</username>"));

                settings = build.getWorkspace()
                        .child(build.getEnvironment(listener).get("GLOBAL_SETTINGS"))
                        .readToString();
                listener.getLogger().println(settings);
                assertThat(settings, containsString("<password>sensitive</password>"));
                assertThat(settings, containsString("<username>admin</username>"));
                return true;
            }
        });
        p.setAssignedNode(slave);
        FreeStyleBuild run = r.buildAndAssertSuccess(p);
        r.assertLogNotContains("<password>bot-user-s3cr3t</password>", run);
        r.assertLogNotContains("<username>bot-user-name</username>", run);
        r.assertLogNotContains("<password>sensitive</password>", run);
        r.assertLogNotContains("<username>admin</username>", run);
        r.assertLogContains("<password>****</password>", run);
        r.assertLogContains("<username>****</username>", run);
    }
}
