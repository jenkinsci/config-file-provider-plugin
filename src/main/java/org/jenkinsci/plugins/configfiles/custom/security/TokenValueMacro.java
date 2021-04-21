/*
 The MIT License

 Copyright (c) 2020, Andrew Grimberg

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
package org.jenkinsci.plugins.configfiles.custom.security;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class TokenValueMacro extends DataBoundTokenMacro {
    @Parameter
    public String token = "";

    @Parameter
    public String value = "";

    public TokenValueMacro(String token, String value) {
        this.token = token;
        this.value = value;
    }

    public TokenValueMacro() {
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(token);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(token);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return this.value;
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return this.value;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultiMap)
            throws MacroEvaluationException, IOException, InterruptedException {
        return this.value;
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultiMap)
            throws MacroEvaluationException, IOException, InterruptedException {
        return this.value;
    }

}
