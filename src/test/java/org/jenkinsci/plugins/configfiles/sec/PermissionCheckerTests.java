package org.jenkinsci.plugins.configfiles.sec;

import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class PermissionCheckerTests {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUpAuthorizationAndProject() {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().
                grant(Jenkins.READ).everywhere().to("reader").
                grant(Jenkins.ADMINISTER).everywhere().to("administer")
        );
    }
    
    @Test
    public void protectedCodeCheckerTest() {
        Runnable run = () -> r.jenkins.checkPermission(Jenkins.ADMINISTER);

        // The administer passes
        PermissionChecker checker = new PermissionChecker(run, "administer");
        checker.assertPass();

        // The reader fails
        checker.withUser("reader").assertFailWithPermission(Jenkins.ADMINISTER);
    }
}
