package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class ConfigFilesActionFactory extends TransientActionFactory<Job> {
    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        if (job instanceof FreeStyleProject || isMavenJob(job)) {
            return Collections.singletonList(new ConfigFilesAction(job));
        }
        return Collections.emptyList();
    }

    private boolean isMavenJob(Job job) {
        try {
            Class<?> mvnJobClass = Class.forName("hudson.maven.MavenModuleSet", false, job.getClass().getClassLoader());
            return mvnJobClass.isAssignableFrom(job.getClass());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
