package org.jenkinsci.plugins.configfiles.sec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.springframework.security.access.AccessDeniedException;

/**
 * Check the {@link ProtectedCodeRunner} class works correctly.
 */
@WithJenkins
class ProtectedCodeRunnerTests {

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
        Supplier<String> supplier = () -> {
            r.jenkins.checkPermission(Jenkins.ADMINISTER);
            return "allowed";
        };

        ProtectedCodeRunner<String> checker = new ProtectedCodeRunner<>(supplier, "administer");
        assertThat(checker.getResult(), is("allowed"));

        Throwable t = checker.withUser("reader").getThrowable();
        assertThat(t, instanceOf(AccessDeniedException.class));
        assertThat(t.getMessage(), containsString(Jenkins.ADMINISTER.group.title + "/" + Jenkins.ADMINISTER.name));
    }
}
