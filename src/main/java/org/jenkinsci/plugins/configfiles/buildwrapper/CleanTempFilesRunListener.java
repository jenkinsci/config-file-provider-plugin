/**
 * 
 */
package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;


/**
 * Removes the temporarily created files at 'onComplete()' of each build, doing it at this state, ensures the files are available also for publishers.
 * 
 * @author Dominik Bartholdi (imod)
 */
@Extension
public class CleanTempFilesRunListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
        CleanTempFilesAction action = build.getAction(CleanTempFilesAction.class);

        if (action != null) {
            for (FilePath fp : action.tempFiles) {
                try {
                    if (fp != null && fp.exists()) {
                        fp.delete();
                    }
                } catch (Exception e) {
                    listener.getLogger().println("[WARN] failed to delete temp file: " + fp.getRemote());
                }
            }
            // remove the action, there is nothing we want to persist on the build
            build.getActions().remove(action);
        }
    }

}
