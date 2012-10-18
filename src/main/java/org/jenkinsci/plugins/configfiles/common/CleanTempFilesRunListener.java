/**
 * 
 */
package org.jenkinsci.plugins.configfiles.common;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.listeners.RunListener;
import hudson.remoting.VirtualChannel;

import java.util.List;

/**
 * Removes the temporarily created files at 'onComplete()' of each build, doing it at this state, ensures the files are available also for publishers.
 * 
 * @author Dominik Bartholdi (imod)
 */
@Extension
public class CleanTempFilesRunListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {

        final List<CleanTempFilesAction> actions = build.getActions(CleanTempFilesAction.class);
        for (CleanTempFilesAction action : actions) {
            try {
                for (String remotePath : action.getTempFiles()) {
                    try {
                        final Node builtOn = build.getBuiltOn();
                        if (builtOn != null) {
                            final VirtualChannel channel = builtOn.getChannel();
                            if (channel != null) {
                                FilePath fp = new FilePath(channel, remotePath);
                                if (fp.exists()) {
                                    fp.delete();
                                }
                            }
                        }
                    } catch (Exception e) {
                        listener.getLogger().println("[WARN] failed to delete temp file: " + remotePath + " - " + e.getMessage());
                    }
                }
            } finally {
                // remove the action, there is nothing we want to persist on the build
                build.getActions().remove(action);
            }
        }

    }
}
