/*
 The MIT License

 Copyright (c) 2023, CloudBees Inc.

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
package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;
import hudson.util.Secret;
import jenkins.util.JenkinsJVM;
import org.apache.commons.beanutils.PropertyUtils;
import org.jenkinsci.plugins.credentialsbinding.masking.SecretPatterns;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Extension to mask any sensitive credentials provided by this plugin in maven settings (local or global) for the maven job type.
 */
@Extension
public class MvnConsoleLogFilter extends ConsoleLogFilter {

    private static final Logger LOGGER = Logger.getLogger(MvnConsoleLogFilter.class.getName());

    @Override
    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
        if (build instanceof AbstractBuild && !(build instanceof FreeStyleBuild)) {
            AbstractProject<?, ?> parent = (AbstractProject<?, ?>) build.getParent();
            if (parent.getClass().getSimpleName().equals("MavenModuleSet")) {
                List<String> secretValues = new ArrayList<>();
                try { //Maven
                    Object settings = PropertyUtils.getProperty(parent, "settings");
                    if (settings instanceof MvnSettingsProvider) {
                        MvnSettingsProvider provider = (MvnSettingsProvider) settings;
                        secretValues.addAll(provider.getSensitiveContentForMasking((AbstractBuild)build));
                    }
                    Object globalSettings = PropertyUtils.getProperty(parent, "globalSettings");
                    if (globalSettings instanceof MvnGlobalSettingsProvider) {
                        MvnGlobalSettingsProvider provider = (MvnGlobalSettingsProvider) globalSettings;
                        secretValues.addAll(provider.getSensitiveContentForMasking((AbstractBuild)build));
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    LOGGER.log(Level.WARNING, "Unable to mask secrets for " + parent.getFullName() + "#" + build.getNumber(), e);
                    PrintStream ps = new PrintStream(logger, false, build.getCharset());
                    e.printStackTrace(ps);
                    ps.flush();
                    assert false : "MavenModuleSet API has changed in an incompatable way";
                }
                if (!secretValues.isEmpty()) {
                    final Secret pattern = Secret.fromString(SecretPatterns.getAggregateSecretPattern(secretValues).pattern());
                    return new SecretPatterns.MaskingOutputStream(logger, 
                            () -> Pattern.compile(pattern.getPlainText()),
                            build.getCharset().name());
                }
            }
        }
        return logger;
    }
}
