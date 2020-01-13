package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class ConfigFilesActionFactory extends TransientActionFactory<AbstractProject> {
    @Override
    public Class<AbstractProject> type() {
        return AbstractProject.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull AbstractProject project) {
        if (project instanceof FreeStyleProject || isMavenJob(project)) {
            return Collections.singletonList(new ConfigFilesAction(project));
        }
        return Collections.emptyList();
    }

    private boolean isMavenJob(AbstractProject project) {
        try {
            Class<?> mvnJobClass = Class.forName("hudson.maven.MavenModuleSet", false, project.getClass().getClassLoader());
            return mvnJobClass.isAssignableFrom(project.getClass());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
