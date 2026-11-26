package org.jenkinsci.plugins.configfiles.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.buildwrapper.ConfigFileBuildWrapper;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.custom.security.CustomizedCredentialMapping;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class CustomConfigTest {

    @TempDir
    public static File folder;

    @Test
    @Issue("SECURITY-3090")
    void credentialsAreMaskedInTheConsole(JenkinsRule r) throws Exception {
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new StringCredentialsImpl(CredentialsScope.GLOBAL, "creds", "desc", Secret.fromString("s3cr3t")));

        GlobalConfigFiles.get()
                .save(new CustomConfig(
                        "my-id",
                        "my-name",
                        "my-comment",
                        "${thecred}",
                        List.of(new CustomizedCredentialMapping("thecred", "creds"))));

        final FreeStyleProject p = r.createFreeStyleProject("free");

        final ManagedFile mCustom = new ManagedFile(
                "my-id", File.createTempFile("junit", null, folder).toString(), null, true);
        ConfigFileBuildWrapper bw = new ConfigFileBuildWrapper(Collections.singletonList(mCustom));
        p.getBuildWrappersList().add(bw);

        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), "s3cr3t"));

        FreeStyleBuild build = r.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());
        r.assertLogNotContains("s3cr3t", build);
    }

    @Test
    void testUsernamePasswordCredentialReplacement(JenkinsRule jr) throws Exception {
        SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "credentialId", "credential description", "admin", "sens1tiv3"));
        GlobalConfigFiles.get().save(new CustomConfig("config-id",
                "config-name",
                "config description",
                """
                  username:${secret_USR}
                  password:${secret_PSW}
                  basic:${secret_BASIC_AUTH}
                """, List.of(new CustomizedCredentialMapping("secret", "credentialId"))));

        final FreeStyleProject p = jr.createFreeStyleProject();
        final ManagedFile mCustom = new ManagedFile("config-id", File.createTempFile("upw", ".txt", folder).toString(), null,
                true);
        p.getBuildWrappersList().add(new ConfigFileBuildWrapper(Collections.singletonList(mCustom)));
        p.getBuildersList().add(new VerifyFileContentBuilder(mCustom.getTargetLocation(), """
                  username:admin
                  password:sens1tiv3
                  basic:YWRtaW46c2VuczF0aXYz
                """));

        FreeStyleBuild build = jr.buildAndAssertSuccess(p);
    }

    private static final class VerifyFileContentBuilder extends Builder {
        private final String filePath;
        private final String expectedContent;

        public VerifyFileContentBuilder(String filePath, String expectedContent) {
            this.filePath = filePath;
            this.expectedContent = expectedContent;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            final String fileContent = Files.readString(new File(filePath).toPath());
            listener.getLogger().println("File contents: ");
            listener.getLogger().println(fileContent);
            assertEquals(expectedContent, fileContent, "file content not correct");
            return true;
        }
    }
}
