/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi, Olivier Lamy

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
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
                    listener.getLogger().println("remotePath: "+remotePath);
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
