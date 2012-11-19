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
package org.jenkinsci.plugins.configfiles.buildwrapper;

import hudson.FilePath;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;

public class ManagedFileUtil {

    /**
     * creates a tmp file on the given channel
     */
    public static FilePath createTempFile(VirtualChannel channel) throws IOException, InterruptedException {
        return channel.call(new Callable<FilePath, IOException>() {
            public FilePath call() throws IOException {
                final File tmpTarget = File.createTempFile("config", "tmp");
                return new FilePath(tmpTarget);
            }

            private static final long serialVersionUID = 1L;
        });
    }

    /**
     * provisions (publishes) the given files to the workspace.
     * 
     * @param managedFiles
     *            the files to be provisioned
     * @param workSpace
     *            target workspace
     * @param logger
     *            the logger
     * @return a map of all the files copied, mapped to the path of the remote location, never <code>null</code>.
     * @throws IOException
     * @throws InterruptedException
     */
    public static Map<ManagedFile, FilePath> provisionConfigFiles(List<ManagedFile> managedFiles, FilePath workSpace, final PrintStream logger)
            throws IOException, InterruptedException {

        final Map<ManagedFile, FilePath> file2Path = new HashMap<ManagedFile, FilePath>();
        logger.println("provisoning config files...");

        for (ManagedFile managedFile : managedFiles) {
            ConfigProvider provider = getProviderForConfigId(managedFile.fileId);

            if (provider == null) {
                throw new IOException(
                        "not able to resolve a provider responsible for the following file - maybe a config-file-provider plugin got deleted by an administrator: "
                                + managedFile);
            }

            Config configFile = provider.getConfigById(managedFile.fileId);
            if (configFile == null) {
                throw new IOException("not able to provide the following file, can't be resolved by any provider - maybe it got deleted by an administrator: "
                        + managedFile);
            }

            boolean createTempFile = StringUtils.isBlank(managedFile.targetLocation);

            FilePath target = null;
            if (createTempFile) {
                target = ManagedFileUtil.createTempFile(workSpace.getChannel());
            } else {
                String targetLocation = managedFile.targetLocation;
                if (!targetLocation.contains(".")) {
                    targetLocation = targetLocation + "/" + configFile.name.replace(" ", "_");
                }
                target = new FilePath(workSpace, targetLocation);
            }

            logger.println(Messages.console_output(configFile.name, target.toURI()));
            ByteArrayInputStream bs = new ByteArrayInputStream(configFile.content.getBytes());
            target.copyFrom(bs);
            file2Path.put(managedFile, target);
        }

        return file2Path;
    }

    private static ConfigProvider getProviderForConfigId(String id) {
        if (!StringUtils.isBlank(id)) {
            for (ConfigProvider provider : ConfigProvider.all()) {
                if (provider.isResponsibleFor(id)) {
                    return provider;
                }
            }
        }
        return null;
    }
}
