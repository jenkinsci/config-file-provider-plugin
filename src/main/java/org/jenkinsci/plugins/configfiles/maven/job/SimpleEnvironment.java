package org.jenkinsci.plugins.configfiles.maven.job;

import hudson.model.Environment;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * A simple environment implementation to provide env variables
 * 
 * @author Dominik Bartholdi (imod)
 */
class SimpleEnvironment extends Environment {

    private final String var;
    private final String value;

    /**
     * Simple constructor for one env variable
     * 
     * @param var
     *            the variable name
     * @param value
     *            the variable value
     */
    public SimpleEnvironment(String var, String value) {
        this.value = value;
        this.var = var;
    }

    @Override
    public void buildEnvVars(Map<String, String> env) {
        if (StringUtils.isNotBlank(var) && StringUtils.isNotBlank(value)) {
            env.put(var, value);
        }
    }
}
