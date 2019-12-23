package org.jenkinsci.plugins.configfiles.properties;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.properties.security.PropertiesCredentialMapping;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

public class PropertiesConfigTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void withCredentials() throws Exception {
        // Smokes with full replace:
        SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "creds", "", "bot", "s3cr3t"));
        GlobalConfigFiles.get().save(new PropertiesConfig("gradle", "gradle", "", "myprop=", true, Collections.singletonList(new PropertiesCredentialMapping("myprop", "creds"))));
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {configFileProvider([configFile(fileId: 'gradle', variable: 'SETTINGS')]) {echo readFile(env.SETTINGS)}}", true));
        r.assertLogContains("myprop=s3cr3t", r.buildAndAssertSuccess(p));
        // Missing credentials. Currently treated as nonfatal:
        SystemCredentialsProvider.getInstance().getCredentials().set(0, new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "creds", "", "bot", "s3cr3t"));
        WorkflowRun b2 = r.buildAndAssertSuccess(p);
        String log = r.getLog(b2);
        System.out.println("####>"+ log);
        System.out.println("<####");
        r.assertLogContains("Could not find credentials ‘creds’ for p #2", b2);
        r.assertLogNotContains("myprop=s3cr3t", b2);
    }
}