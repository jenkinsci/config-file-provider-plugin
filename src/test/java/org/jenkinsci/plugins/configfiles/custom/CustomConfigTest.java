package org.jenkinsci.plugins.configfiles.custom;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.ConfigFileBuildWrapper;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.custom.security.CustomizedCredentialMapping;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Cause.UserIdCause;
import hudson.tasks.Builder;
import hudson.util.Secret;

public class CustomConfigTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test @Issue("SECURITY-3090")
    public void credentialsAreMaskedInTheConsole() throws Exception {
        SystemCredentialsProvider.getInstance().getCredentials().add(new StringCredentialsImpl(CredentialsScope.GLOBAL, "creds", "desc", Secret.fromString("s3cr3t")));

        GlobalConfigFiles.get().save(new CustomConfig("my-id", "my-name", "my-comment", "${thecred}", List.of(new CustomizedCredentialMapping("thecred", "creds"))));

        final FreeStyleProject p = r.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile("my-id", folder.newFile().toString(), null, true);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "s3cr3t"));

        FreeStyleBuild build = r.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
        r.assertLogNotContains("s3cr3t", build);
    }

    private static final class VerifyFileContentBuilder extends Builder {
        private final String filePath;
        private final String expectedContent;

        public VerifyFileContentBuilder(String filePath, String expectedContent) {
            this.filePath = filePath;
            this.expectedContent = expectedContent;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            final String fileContent = IOUtils.toString(new FileReader(filePath));
            listener.getLogger().println("File contents: ");
            listener.getLogger().println(fileContent);
            Assert.assertEquals("file content not correct", expectedContent, fileContent);
            return true;
        }
    }
}
