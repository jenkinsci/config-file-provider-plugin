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
package org.jenkinsci.lib.configprovider.model;

import java.io.Serializable;

import hudson.Util;

public class ConfigFile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fileId;
    protected String targetLocation;
    protected boolean replaceTokens;

    public ConfigFile(String fileId, String targetLocation, boolean replaceTokens) {
        this.fileId = fileId;
        this.targetLocation = Util.fixEmptyAndTrim(targetLocation);
        this.replaceTokens = replaceTokens;
    }

    public String getFileId() {
        return fileId;
    }

    public String getTargetLocation() {
        return this.targetLocation;
    }

    public boolean isReplaceTokens() {
        return replaceTokens;
    }

}