package org.jenkinsci.plugins.configfiles.sec;

import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PermissionCheckerTests {

    private JenkinsRule r;

    @BeforeEach
    void setUpAuthorizationAndProject(JenkinsRule r) {
        this.r = r;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("reader")
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("administer"));
    }

    @Test
    void protectedCodeCheckerTest() {
        Runnable run = () -> r.jenkins.checkPermission(Jenkins.ADMINISTER);

        // The administer passes
        PermissionChecker checker = new PermissionChecker(run, "administer");
        checker.assertPass();

        // The reader fails
        checker.withUser("reader").assertFailWithPermission(Jenkins.ADMINISTER);
    }
}
