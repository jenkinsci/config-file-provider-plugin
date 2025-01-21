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

import static org.junit.Assert.assertNotNull;
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
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

public class ConfigFileBuildWrapperWorkflowTest {

    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Test
    @WithJenkins
    void configRoundTrip(JenkinsRule rule) throws Exception {
        String id = createConfig(rule).id;
        CoreWrapperStep step = new CoreWrapperStep(
                new ConfigFileBuildWrapper(Collections.singletonList(new ManagedFile(id, "myfile.txt", "MYFILE"))));
        step = new StepConfigTester(rule).configRoundTrip(step);
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
        rule.assertEqualDataBoundBeans(w, model.instantiate(args));
    }

    @Test
    @WithJenkins
    void withTargetLocation_Pipeline(JenkinsRule rule) throws Exception {
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                ""
                        + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                        + "node {\n"
                        + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '" + createConfig(rule).id
                        + "', targetLocation: 'myfile.txt']]]) {\n"
                        + "    xsh 'myfile.txt'\n"
                        + "  }\n"
                        + "}",
                true));
        WorkflowRun b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        rule.assertLogContains("some content", b);
        rule.assertLogNotContains("temporary files", b);
    }

    @Test
    @WithJenkins
    void symbolWithTargetLocation(JenkinsRule rule) throws Exception {
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                ""
                        + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                        + "node {\n"
                        + "  configFileProvider([configFile(fileId: '" + createConfig(rule).id
                        + "', targetLocation: 'myfile.txt')]) {\n"
                        + "    xsh 'myfile.txt'\n"
                        + "  }\n"
                        + "}",
                true));
        WorkflowRun b = rule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        rule.assertLogContains("some content", b);
        rule.assertLogNotContains("temporary files", b);
    }

    @org.junit.Test
    public void withTempFilesAfterRestart() {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(
                        ""
                                + "node {\n"
                                + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '"
                                + createConfig(story.j).id + "', variable: 'MYFILE']]]) {\n"
                                + "    semaphore 'withTempFilesAfterRestart'\n"
                                + "    if (isUnix()) {sh 'cat \"$MYFILE\"'} else {bat 'type \"%MYFILE%\"'}\n"
                                + "  }\n"
                                + "}",
                        true));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                SemaphoreStep.waitForStart("withTempFilesAfterRestart/1", b);
            }
        });
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SemaphoreStep.success("withTempFilesAfterRestart/1", null);
                WorkflowJob p = story.j.jenkins.getItemByFullName("p", WorkflowJob.class);
                assertNotNull(p);
                WorkflowRun b = p.getBuildByNumber(1);
                assertNotNull(b);
                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b));
                story.j.assertLogContains("some content", b);
                story.j.assertLogContains("Deleting 1 temporary files", b);
            }
        });
    }

    private Config createConfig(JenkinsRule rule) {
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
