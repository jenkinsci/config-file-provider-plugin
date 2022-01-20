package org.jenkinsci.plugins.configfiles;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import java.util.Collections;

/**
 * attaches an action to handle previews of a config file
 */
@Extension
public class ConfigFilesActionFactory extends TransientActionFactory<Job> {
    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull Job job) {
        return Collections.singletonList(new ConfigFilesAction(job));
    }
}
