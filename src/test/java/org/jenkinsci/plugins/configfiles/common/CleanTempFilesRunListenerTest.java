package org.jenkinsci.plugins.configfiles.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jakarta.inject.Inject;
import java.util.Arrays;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.builder.ConfigFileBuildStep;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CleanTempFilesRunListenerTest {

    @Inject
    CustomConfigProvider customConfigProvider;

    @Test
    void cleanTempFilesActionIsTransient(JenkinsRule j) throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final FreeStyleProject p = j.createFreeStyleProject("free");

        final Config customFile = createCustomFile(j, "config-id", customConfigProvider, "blah");
        final ManagedFile managedFile = new ManagedFile(customFile.id, "/tmp/blah", null, true);
        p.getBuildersList().add(new ConfigFileBuildStep(Arrays.asList(managedFile)));

        p.getBuildersList().add(new AssertOneCleanTempFilesAction());

        j.assertBuildStatus(
                Result.SUCCESS, p.scheduleBuild2(0, new UserIdCause()).get());

        // assert the CleanTempFilesAction is gone
        assertEquals(
                0, p.getBuildByNumber(1).getActions(CleanTempFilesAction.class).size());
    }

    private Config createCustomFile(JenkinsRule j, String configId, CustomConfigProvider provider, String content) {
        Config c1 = provider.newConfig(configId);
        c1 = new CustomConfig(c1.id, c1.name, c1.comment, content);
        GlobalConfigFiles globalConfigFiles =
                j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
    }

    private static class AssertOneCleanTempFilesAction extends TestBuilder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            // assert the CleanTempFilesAction is there
            assertEquals(1, build.getActions(CleanTempFilesAction.class).size());
            return true;
        }
    }
}
