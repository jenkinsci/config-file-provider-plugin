package org.jenkinsci.plugins.configfiles.sec;

import hudson.security.AccessDeniedException2;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Check the {@link ProtectedCodeRunner} class works correctly.
 */
public class ProtectedCodeRunnerTests {
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
        Supplier<String> supplier = () -> {
            r.jenkins.checkPermission(Jenkins.ADMINISTER);
            return "allowed";
        };
        
        ProtectedCodeRunner<String> checker = new ProtectedCodeRunner<>(supplier, "administer");
        assertThat(checker.getResult(), is("allowed"));

        Throwable t = checker.withUser("reader").getThrowable(); 
        assertThat(t, instanceOf(AccessDeniedException2.class));
        assertThat(((AccessDeniedException2) t).permission, equalTo(Jenkins.ADMINISTER));
    }
}
