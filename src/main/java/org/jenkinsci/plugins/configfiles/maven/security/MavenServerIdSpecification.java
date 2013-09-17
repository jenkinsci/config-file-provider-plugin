package org.jenkinsci.plugins.configfiles.maven.security;

import hudson.Extension;
import hudson.Util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.DomainSpecificationDescriptor;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author Dominik Bartholdi (imod)
 * 
 */
public class MavenServerIdSpecification extends DomainSpecification {

    /**
     * ServerIds to match. A comma separated set of <var>serverId</var> with <code>*</code> wildcards supported. {@code null} signifies include everything.
     */
    @CheckForNull
    private final String includes;

    /**
     * ServerIds to explicitly not match. A comma separated set of <var>serverId</var> with <code>*</code> wildcards supported. {@code null} signifies exclude nothing.
     */
    @CheckForNull
    private final String excludes;

    /**
     * Constructor for stapler.
     * 
     * @param includes
     *            ServerIds to match. A comma separated set of <var>serverId</var> with <code>*</code> wildcards supported. {@code null} signifies include everything.
     * @param excludes
     *            ServerIds to explicitly not match. A comma separated set of <var>serverId</var> with <code>*</code> wildcards supported. {@code null} signifies exclude nothing.
     */
    @DataBoundConstructor
    @SuppressWarnings("unused")
    // by stapler
    public MavenServerIdSpecification(String includes, String excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getIncludes() {
        return includes;
    }

    /**
     * @see com.cloudbees.plugins.credentials.domains.DomainSpecification#test(com.cloudbees.plugins.credentials.domains.DomainRequirement)
     */
    @Override
    public Result test(DomainRequirement requirement) {
        if (requirement instanceof MavenServerIdRequirement) {
            String serverId = ((MavenServerIdRequirement) requirement).getServerId();
            if (includes != null) {
                boolean isInclude = false;
                for (String include : includes.split("[,\\n ]")) {
                    include = Util.fixEmptyAndTrim(include);
                    if (include == null) {
                        continue;
                    }
                    if (FilenameUtils.wildcardMatch(serverId, include, IOCase.INSENSITIVE)) {
                        isInclude = true;
                        break;
                    }
                }
                if (!isInclude) {
                    return Result.NEGATIVE;
                }
            }
            if (excludes != null) {
                boolean isExclude = false;
                for (String exclude : excludes.split("[,\\n ]")) {
                    exclude = Util.fixEmptyAndTrim(exclude);
                    if (exclude == null) {
                        continue;
                    }
                    if (FilenameUtils.wildcardMatch(serverId, exclude, IOCase.INSENSITIVE)) {
                        isExclude = true;
                        break;
                    }
                }
                if (isExclude) {
                    return Result.NEGATIVE;
                }
            }
            return Result.PARTIAL;
        }
        return Result.UNKNOWN;
    }

    /**
     * {@link hudson.model.Descriptor}.
     */
    @Extension
    @SuppressWarnings("unused")
    // by stapler
    public static class DescriptorImpl extends DomainSpecificationDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.MavenServerIdSpecification_displayname();
        }
    }
}
