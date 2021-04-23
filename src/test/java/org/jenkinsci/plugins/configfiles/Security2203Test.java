package org.jenkinsci.plugins.configfiles;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.Permission;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.job.MvnGlobalSettingsProvider;
import org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider;
import org.jenkinsci.plugins.configfiles.sec.ProtectedCodeRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;

/**
 * Testing there is no information disclosure.
 */
public class Security2203Test {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Inject
    MavenSettingsConfig.MavenSettingsConfigProvider mavenSettingConfigProvider;
    
    @Inject
    GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;
    
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
     * The {@link MvnSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item, String)} is only accessible by 
     * administers.
     */
    @Issue({"SECURITY-2203", "JENKINS-65436"})
    @Test
    public void mvnSettingsProviderDoFillSettingsConfigIdItemsProtectedGlobalConfiguration() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(mavenSettingConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnSettingsProvider.DescriptorImpl descriptor = (MvnSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), null, CURRENT); // no project, global configuration

        };
        ProtectedCodeRunner<ListBoxModel> projectChecker = new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

        ListBoxModel result;

        // Reader doesn't get the list of credentials
        result = projectChecker.getResult(); // with reader
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));

        // projectConfigurer doesn't get the list of credentials
        result = projectChecker.withUser("projectConfigurer").getResult(); // with projectConfigurer
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));
        
        result = projectChecker.withUser("administer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned
    }

    /**
     * The {@link MvnSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item, String)} is only accessible by people able to
     * configure the job.
     */
    @Issue({"SECURITY-2203", "JENKINS-65436"})
    @Test
    public void mvnSettingsProviderDoFillSettingsConfigIdItemsProtectedForProject() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(mavenSettingConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnSettingsProvider.DescriptorImpl descriptor = (MvnSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project, CURRENT); // we pass the project

        };
        ProtectedCodeRunner<ListBoxModel> projectChecker = new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

        ListBoxModel result;

        // Reader doesn't get the list of credentials
        result = projectChecker.getResult(); // with reader
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));

        // projectConfigurer can configure the project
        result = projectChecker.withUser("projectConfigurer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned

        // administer can configure the project
        result = projectChecker.withUser("administer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned
    }

    /**
     * The {@link MvnGlobalSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item, String)} is only accessible by people able to
     * administer Jenkins.
     */
    @Issue({"SECURITY-2203", "JENKINS-65436"})
    @Test
    public void mvnGlobalSettingsProviderDoFillSettingsConfigIdItemsProtectedGlobalConfiguration() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(globalMavenSettingsConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnGlobalSettingsProvider.DescriptorImpl descriptor = (MvnGlobalSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnGlobalSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), null, CURRENT); // from global configuration, no project

        };
        ProtectedCodeRunner<ListBoxModel> projectChecker = new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

        ListBoxModel result;

        // Reader doesn't get the list of credentials
        result = projectChecker.getResult(); // with reader
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));

        // projectConfigurer doesn't get the list of credentials
        result = projectChecker.withUser("projectConfigurer").getResult(); // with projectConfigurer
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));

        result = projectChecker.withUser("administer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned
    }

    /**
     * The {@link MvnGlobalSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item, String)} is only accessible by people able to
     * configure the job.
     */
    @Issue({"SECURITY-2203", "JENKINS-65436"})
    @Test
    public void mvnGlobalSettingsProviderDoFillSettingsConfigIdItemsProtectedForProject() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(globalMavenSettingsConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnGlobalSettingsProvider.DescriptorImpl descriptor = (MvnGlobalSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnGlobalSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project, CURRENT); // we pass the project

        };
        ProtectedCodeRunner<ListBoxModel> projectChecker = new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

        ListBoxModel result;

        // Reader doesn't get the list of credentials
        result = projectChecker.getResult(); // with reader
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, equalTo(CURRENT));

        // projectConfigurer can configure the project
        result = projectChecker.withUser("projectConfigurer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned

        // administer can configure the project
        result = projectChecker.withUser("administer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).value, equalTo(""));
        assertThat(result.get(1).value, startsWith(c.id)); // The config created is successfully returned
    }
    
    private Config createSetting(ConfigProvider provider) {
        Config c1 = provider.newConfig();
        GlobalConfigFiles globalConfigFiles = r.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;
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
        } catch (AccessDeniedException e) {
            assertThat(e.getMessage(), containsString(permission.group.title + "/" + permission.name));
        }

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(userWithPermission.get(permission)))) {
            run.run(); // The method doesn't fail
        } catch (AccessDeniedException e) {
            fail(String.format("%s should be accessible to people with the permission %s but it failed with the exception: %s", checkedMethod, permission, e));
        }
    }
}
