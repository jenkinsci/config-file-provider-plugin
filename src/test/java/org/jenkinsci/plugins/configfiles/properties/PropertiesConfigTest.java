package org.jenkinsci.plugins.configfiles.properties;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import java.util.Collections;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.properties.security.PropertiesCredentialMapping;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PropertiesConfigTest {

    @Test
    void withCredentials(JenkinsRule r) throws Exception {
        // Smokes with full replace:
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL, "creds", "", "bot-user-name", "bot-user-s3cr3t"));
        GlobalConfigFiles.get()
                .save(new PropertiesConfig(
                        "gradle",
                        "gradle",
                        "",
                        "myprop=",
                        true,
                        Collections.singletonList(new PropertiesCredentialMapping("myprop", "creds"))));
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                String.join(
                        "\n",
                        "node {",
                        "  configFileProvider([configFile(fileId: 'gradle', ",
                        "                                 variable: 'SETTINGS')]) {",
                        "    String content = readFile(env.SETTINGS)",
                        "    if (currentBuild.id == 1) { // only the first build will have the secret",
                        "      assert content.contains('myprop=bot-user-s3cr3t')",
                        "    }",
                        "    echo content",
                        "  }",
                        "}"),
                true));
        WorkflowRun b1 = r.buildAndAssertSuccess(p);
        r.assertLogContains("myprop=****", b1);
        r.assertLogNotContains("myprop=bot-user-s3cr3t", b1);
        // Missing credentials. Currently treated as nonfatal:
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .set(
                        0,
                        new UsernamePasswordCredentialsImpl(
                                CredentialsScope.SYSTEM, "creds", "", "bot-user-name", "bot-user-s3cr3t"));
        WorkflowRun b2 = r.buildAndAssertSuccess(p);
        r.assertLogContains("Could not find credentials [creds] for p #2", b2);
        r.assertLogContains("myprop=" + System.lineSeparator(), b2);
    }
}
