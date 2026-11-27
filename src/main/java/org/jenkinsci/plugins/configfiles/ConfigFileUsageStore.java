/*
 The MIT License

 Copyright (c) 2011, Dominik Bartholdi

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
package org.jenkinsci.plugins.configfiles;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.XmlFile;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.Run;
import hudson.util.XStream2;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records, per "location" (the Jenkins root directory for globally scoped configuration
 * files, or an individual folder's root directory for folder scoped ones), which jobs used
 * which configuration file - identified only by its id, never by its content - and in which
 * builds.
 * <p>
 * Each location gets its own small, dedicated file (separate from the configuration files
 * themselves / the folder properties), so that recording a usage never requires rewriting
 * the (potentially large) list of configuration files on every build.
 */
@Restricted(NoExternalUse.class)
public class ConfigFileUsageStore {

    private static final Logger LOGGER = Logger.getLogger(ConfigFileUsageStore.class.getName());

    private static final String FILE_NAME = "config-file-provider-usage.xml";

    private static final XStream2 XSTREAM = new XStream2();

    private static final Map<File, ConfigFileUsageStore> INSTANCES = new ConcurrentHashMap<>();

    private final XmlFile file;

    /** configId -> jobFullName -> build numbers that used it */
    private Map<String, Map<String, SortedSet<Integer>>> usages;

    private ConfigFileUsageStore(File rootDir) {
        this.file = new XmlFile(XSTREAM, new File(rootDir, FILE_NAME));
    }

    /**
     * Returns the (cached) usage store responsible for the given location, identified by its
     * root directory, e.g. {@code Jenkins.get().getRootDir()} for the global scope, or
     * {@code folder.getRootDir()} for a folder.
     */
    public static ConfigFileUsageStore forRootDir(@NonNull File rootDir) {
        return INSTANCES.computeIfAbsent(rootDir, ConfigFileUsageStore::new);
    }

    private synchronized Map<String, Map<String, SortedSet<Integer>>> load() {
        if (usages == null) {
            Map<String, Map<String, SortedSet<Integer>>> loaded = null;
            if (file.exists()) {
                try {
                    loaded = (Map<String, Map<String, SortedSet<Integer>>>) file.read();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not load configuration file usage data from " + file, e);
                }
            }
            usages = loaded != null ? loaded : new TreeMap<>();
        }
        return usages;
    }

    /**
     * Records that the given build used the configuration file with the given id.
     */
    public synchronized void trackUsage(@NonNull String configId, @NonNull Run<?, ?> run) {
        Map<String, Map<String, SortedSet<Integer>>> all = load();
        Map<String, SortedSet<Integer>> jobs = all.computeIfAbsent(configId, k -> new TreeMap<>());
        SortedSet<Integer> builds = jobs.computeIfAbsent(run.getParent().getFullName(), k -> new TreeSet<>());
        if (builds.add(run.getNumber())) {
            save();
        }
    }

    /**
     * Returns the recorded usage of the configuration file with the given id, as a map of job
     * full name to the {@link RangeSet} of build numbers that used it.
     */
    @NonNull
    public synchronized Map<String, RangeSet> getUsage(@NonNull String configId) {
        Map<String, SortedSet<Integer>> jobs = load().get(configId);
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, RangeSet> result = new TreeMap<>();
        for (Map.Entry<String, SortedSet<Integer>> entry : jobs.entrySet()) {
            RangeSet rangeSet = new RangeSet();
            for (Integer build : entry.getValue()) {
                rangeSet.add(build);
            }
            result.put(entry.getKey(), rangeSet);
        }
        return result;
    }

    private void save() {
        try {
            file.write(usages);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save configuration file usage data to " + file, e);
        }
    }
}
