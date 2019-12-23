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
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigFileBuildWrapperWorkflowTest {

    @Rule public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule public BuildWatcher buildWatcher = new BuildWatcher();

    @Test public void configRoundTrip() {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                String id = createConfig().id;
                CoreWrapperStep step = new CoreWrapperStep(new ConfigFileBuildWrapper(Collections.singletonList(new ManagedFile(id, "myfile.txt", "MYFILE"))));
                step = new StepConfigTester(story.j).configRoundTrip(step);
                SimpleBuildWrapper delegate = step.getDelegate();
                assertTrue(String.valueOf(delegate), delegate instanceof ConfigFileBuildWrapper);
                ConfigFileBuildWrapper w = (ConfigFileBuildWrapper) delegate;
                assertEquals("[[ManagedFile: id=" + id + ", targetLocation=myfile.txt, variable=MYFILE]]", w.getManagedFiles().toString());

                // test pipeline snippet generator
                List<ManagedFile> managedFiles = new ArrayList<ManagedFile>();
                managedFiles.add(new ManagedFile("myid"));
                w = new ConfigFileBuildWrapper(managedFiles);

                DescribableModel<ConfigFileBuildWrapper> model = new DescribableModel<ConfigFileBuildWrapper>(ConfigFileBuildWrapper.class);
                Map<String, Object> args = model.uninstantiate(w);
                assertEquals(1, ((List)args.get("managedFiles")).size());
                story.j.assertEqualDataBoundBeans(w,model.instantiate(args));
            }
        });
    }

    @Test public void withTargetLocation_Pipeline() throws Exception {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(""
                        + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                        + "node {\n"
                        + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '" + createConfig().id + "', targetLocation: 'myfile.txt']]]) {\n"
                        + "    xsh 'myfile.txt'\n"
                        + "  }\n"
                        + "}", true));
                WorkflowRun b = story.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
                story.j.assertLogContains("some content", b);
                story.j.assertLogNotContains("temporary files", b);
            }
        });
    }

    @Test public void symbolWithTargetLocation() throws Exception {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(""
                        + "def xsh(file) { if (isUnix()) {sh \"cat $file\"} else {bat \"type $file\"} }\n"
                        + "node {\n"
                        + "  configFileProvider([configFile(fileId: '" + createConfig().id + "', targetLocation: 'myfile.txt')]) {\n"
                        + "    xsh 'myfile.txt'\n"
                        + "  }\n"
                        + "}", true));
                WorkflowRun b = story.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
                story.j.assertLogContains("some content", b);
                story.j.assertLogNotContains("temporary files", b);
            }
        });
    }

    @Test public void withTempFilesAfterRestart() throws Exception {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(""
                        + "node {\n"
                        + "  wrap([$class: 'ConfigFileBuildWrapper', managedFiles: [[fileId: '" + createConfig().id + "', variable: 'MYFILE']]]) {\n"
                        + "    semaphore 'withTempFilesAfterRestart'\n"
                        + "    if (isUnix()) {sh 'cat \"$MYFILE\"'} else {bat 'type \"%MYFILE%\"'}\n"
                        + "  }\n"
                        + "}", true));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                SemaphoreStep.waitForStart("withTempFilesAfterRestart/1", b);
            }
        });
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
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

    private Config createConfig() {
        ConfigProvider configProvider = story.j.jenkins.getExtensionList(ConfigProvider.class).get(CustomConfig.CustomConfigProvider.class);
        String id = configProvider.getProviderId() + "myfile";
        Config config = new CustomConfig(id, "My File", "", "some content");

        GlobalConfigFiles globalConfigFiles = story.j.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(config);
        return config;
    }

}
