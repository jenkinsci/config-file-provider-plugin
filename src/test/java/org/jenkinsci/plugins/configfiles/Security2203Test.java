package org.jenkinsci.plugins.configfiles;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.maven.job.MvnGlobalSettingsProvider;
import org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

/**
 * Testing there is no information disclosure.
 */
public class Security2203Test {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    private FreeStyleProject project;

    @Before
    public void setUpAuthorizationAndProject() throws IOException {
        project = r.jenkins.createProject(FreeStyleProject.class, "j");

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().
                grant(Jenkins.READ, Item.READ).everywhere().to("reader").
                grant(Item.CONFIGURE).onItems(project).to("projectConfigurer").
                grant(Jenkins.ADMINISTER).everywhere().to("administer")
        );
    }

    /**
     * The {@link ManagedFile.DescriptorImpl#doFillFileIdItems(ItemGroup, Item)} is only accessible by people able to
     * configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    public void managedFileDoFillFiledIdItemsProtected() {
        Runnable run = () -> {
            ManagedFile.DescriptorImpl descriptor = (ManagedFile.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ManagedFile.class);
            descriptor.doFillFileIdItems(Jenkins.get(), project);
        };

        assertWhoCanExecute(run,Item.CONFIGURE, "ManagedFile.DescriptorImpl#doFillFileIdItems");
    }

    /**
     * The {@link ManagedFile.DescriptorImpl#doCheckFileId(StaplerRequest, Item, String)} is only accessible by people
     * able to configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    public void managedFileDoCheckFileIdProtected() {
        Runnable run = () -> {
            ManagedFile.DescriptorImpl descriptor = (ManagedFile.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ManagedFile.class);
            descriptor.doCheckFileId(null, project, "fileId"); // request won't be used, we can use null
        };

        assertWhoCanExecute(run, Item.CONFIGURE, "ManagedFile.DescriptorImpl#doCheckFileId");
    }
    
    /**
     * The {@link MvnGlobalSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item)} is only accessible by people able to
     * configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    public void mvnGlobalSettingsProviderDoFillSettingsConfigIdItemsProtected() {
        Runnable run = () -> {
            MvnGlobalSettingsProvider.DescriptorImpl descriptor = (MvnGlobalSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnGlobalSettingsProvider.class);
            descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project);
        };
        
        assertWhoCanExecute(run, Item.CONFIGURE, "MvnGlobalSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems");
    }

    /**
     * The {@link MvnSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item)} is only accessible by people able to
     * configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    public void mvnSettingsProviderDoFillSettingsConfigIdItemsProtected() {
        Runnable run = () -> {
            MvnSettingsProvider.DescriptorImpl descriptor = (MvnSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnSettingsProvider.class);
            descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project);
        };

        assertWhoCanExecute(run, Item.CONFIGURE, "MvnSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems");
    }

    /**
     * The {@link ConfigFilesManagement#getTarget()} is only accessible by people able to administer jenkins. It guarantees
     * all methods in the class require {@link Jenkins#ADMINISTER}.
     */
    @Issue("SECURITY-2203")
    @Test
    public void configFilesManagementAllMethodsProtected() {
        Runnable run = () -> {
            ConfigFilesManagement configFilesManagement = Jenkins.get().getExtensionList(ConfigFilesManagement.class).get(0);
            configFilesManagement.getTarget();
        };

        assertWhoCanExecute(run, Jenkins.ADMINISTER, "ConfigFilesManagement#getTarget");
    }
    
    /**
     * Common logic to check a specific method is accessible by people with Configure permission and not by any person 
     * with just read permission. We don't care about the result. If you don't have permission, the method with fail.
     * @param run The method to check.
     * @param checkedMethod The name of the method for logging purposes.
     */
    private void assertWhoCanExecute(Runnable run, Permission permission, String checkedMethod) {
        final Map<Permission, String> userWithPermission = Stream.of(
                new AbstractMap.SimpleEntry<>(Jenkins.READ, "reader"),
                new AbstractMap.SimpleEntry<>(Item.CONFIGURE, "projectConfigurer"),
                new AbstractMap.SimpleEntry<>(Jenkins.ADMINISTER, "administer"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName("reader"))) {
            run.run(); // The method should fail
            fail(String.format("%s should be only accessible by people with the permission %s, but it's accessible by a person with %s", checkedMethod, permission, Item.READ));
        } catch (AccessDeniedException2 e) {
            assertThat(e.permission, equalTo(permission));
        }

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(userWithPermission.get(permission)))) {
            run.run(); // The method doesn't fail
        } catch (AccessDeniedException2 e) {
            fail(String.format("%s should be accessible to people with the permission %s but it failed with the exception: %s", checkedMethod, permission, e));
        }
    }
}
