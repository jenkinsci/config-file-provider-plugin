/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
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

package org.jenkinsci.plugins.configfiles.buildwrapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.CoreWrapperStep;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.JenkinsSessionExtension;

class ConfigFileBuildWrapperWorkflowTest {

    @RegisterExtension
    private final JenkinsSessionExtension story = new JenkinsSessionExtension();

    @Test
    void configRoundTrip() throws Throwable {
        story.then(j -> {
            String id = createConfig(j).id;
            CoreWrapperStep step = new CoreWrapperStep(
                    new ConfigFileBuildWrapper(Collections.singletonList(new ManagedFile(id, "myfile.txt", "MYFILE"))));
            step = new StepConfigTester(j).configRoundTrip(step);
            SimpleBuildWrapper delegate = step.getDelegate();
            assertInstanceOf(ConfigFileBuildWrapper.class, delegate, String.valueOf(delegate));
            ConfigFileBuildWrapper w = (ConfigFileBuildWrapper) delegate;
            assertEquals(
                    "[[ManagedFile: id=" + id + ", targetLocation=myfile.txt, variable=MYFILE]]",
                    w.getManagedFiles().toString());

            // test pipeline snippet generator
            List<ManagedFile> managedFiles = new ArrayList<>();
            managedFiles.add(new ManagedFile("myid"));
            w = new ConfigFileBuildWrapper(managedFiles);

            DescribableModel<ConfigFileBuildWrapper> model = new DescribableModel<>(ConfigFileBuildWrapper.class);
            Map<String, Object> args = model.uninstantiate(w);
            assertEquals(1, ((List) args.get("managedFiles")).size());
            j.assertEqualDataBoundBeans(w, model.instantiate(args));
        });
    }

    @Test
    void withTargetLocation_Pipeline() throws Throwable {
        story.then(j -> {
            WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    ""
                            + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                            + "node {\n"
                            + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '" + createConfig(j).id
                            + "', targetLocation: 'myfile.txt']]]) {\n"
                            + "    xsh 'myfile.txt'\n"
                            + "  }\n"
                            + "}",
                    true));
            WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            j.assertLogContains("some content", b);
            j.assertLogNotContains("temporary files", b);
        });
    }

    @Test
    void symbolWithTargetLocation() throws Throwable {
        story.then(j -> {
            WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    ""
                            + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                            + "node {\n"
                            + "  configFileProvider([configFile(fileId: '" + createConfig(j).id
                            + "', targetLocation: 'myfile.txt')]) {\n"
                            + "    xsh 'myfile.txt'\n"
                            + "  }\n"
                            + "}",
                    true));
            WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            j.assertLogContains("some content", b);
            j.assertLogNotContains("temporary files", b);
        });
    }

    @Test
    void withTempFilesAfterRestart() throws Throwable {
        story.then(j -> {
            WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    ""
                            + "node {\n"
                            + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '"
                            + createConfig(j).id + "', variable: 'MYFILE']]]) {\n"
                            + "    semaphore 'withTempFilesAfterRestart'\n"
                            + "    if (isUnix()) {sh 'cat \"$MYFILE\"'} else {bat 'type \"%MYFILE%\"'}\n"
                            + "  }\n"
                            + "}",
                    true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            SemaphoreStep.waitForStart("withTempFilesAfterRestart/1", b);
        });
        story.then(j -> {
            SemaphoreStep.success("withTempFilesAfterRestart/1", null);
            WorkflowJob p = j.jenkins.getItemByFullName("p", WorkflowJob.class);
            assertNotNull(p);
            WorkflowRun b = p.getBuildByNumber(1);
            assertNotNull(b);
            j.assertBuildStatusSuccess(j.waitForCompletion(b));
            j.assertLogContains("some content", b);
            j.assertLogContains("Deleting 1 temporary files", b);
        });
    }

    private static Config createConfig(JenkinsRule rule) {
        ConfigProvider configProvider =
                rule.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        String id = configProvider.getProviderId() + "myfile";
        Config config = new CustomConfig(id, "My File", "", "some content");

        GlobalConfigFiles globalConfigFiles =
                rule.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }
}
