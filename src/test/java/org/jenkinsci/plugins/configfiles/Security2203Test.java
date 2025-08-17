package org.jenkinsci.plugins.configfiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.job.MvnGlobalSettingsProvider;
import org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider;
import org.jenkinsci.plugins.configfiles.sec.ProtectedCodeRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.springframework.security.access.AccessDeniedException;

/**
 * Testing there is no information disclosure.
 */
@WithJenkins
class Security2203Test {

    private JenkinsRule r;

    @Inject
    CustomConfig.CustomConfigProvider customConfigProvider;

    @Inject
    MavenSettingsConfig.MavenSettingsConfigProvider mavenSettingConfigProvider;

    @Inject
    GlobalMavenSettingsConfig.GlobalMavenSettingsConfigProvider globalMavenSettingsConfigProvider;

    private FreeStyleProject project;

    @BeforeEach
    void setUpAuthorizationAndProject(JenkinsRule r) throws IOException {
        this.r = r;
        project = r.jenkins.createProject(FreeStyleProject.class, "j");

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ)
                .everywhere()
                .to("reader")
                .grant(Item.CONFIGURE)
                .onItems(project)
                .to("projectConfigurer")
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("administer"));
    }

    /**
     * The {@link ManagedFile.DescriptorImpl#doFillFileIdItems(ItemGroup, Item, AccessControlled, String)} is only accessible by people able to
     * configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    void managedFileDoFillFiledIdItemsProtected() {
        r.jenkins.getInjector().injectMembers(this);
        final Config c = createSetting(customConfigProvider, "fileId");
        final String CURRENT = "current-value";

        Callable<ListBoxModel> run = () -> {
            ManagedFile.DescriptorImpl descriptor =
                    (ManagedFile.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ManagedFile.class);
            return descriptor.doFillFileIdItems(Jenkins.get(), project, project, CURRENT);
        };

        assertWhoCanExecute(run, "ManagedFile.DescriptorImpl#doFillFileIdItems", result ->
                {
                    assertThat(result, hasSize(2));
                    assertThat(result.get(0).value, equalTo(""));
                    assertThat(result.get(1).value, equalTo(CURRENT));
                }, result -> {
                    assertThat(result, hasSize(2));
                    assertThat(result.get(0).value, equalTo(""));
                    assertThat(result.get(1).value, equalTo(c.id));
                }
        );
    }

    /**
     * The {@link ManagedFile.DescriptorImpl#doCheckFileId(StaplerRequest2, Item, AccessControlled, String)} is only accessible by people
     * able to configure the job.
     */
    @Issue("SECURITY-2203")
    @Test
    void managedFileDoCheckFileIdProtected() {
        Callable<HttpResponse> run = () -> {
            ManagedFile.DescriptorImpl descriptor =
                    (ManagedFile.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ManagedFile.class);
            return descriptor.doCheckFileId(null, project, project, "fileId"); // request won't be used, we can use null
        };

        assertWhoCanExecute(run, "ManagedFile.DescriptorImpl#doCheckFileId",
                result -> {
                    if (result instanceof FormValidation v) {
                        assertThat(v.kind, equalTo(FormValidation.Kind.OK)); // The user has no permission so it must be of Kind.OK
                    } else {
                        fail("Expected FormValidation but got: " + result.getClass().getName());
                    }
                },
                result -> {
                    if (result instanceof FormValidation v) {
                        assertThat(v.kind, equalTo(FormValidation.Kind.ERROR)); // The user has permission but the fileId doesn't exist -> Kind.ERROR
                    } else {
                        fail("Expected FormValidation but got: " + result.getClass().getName());
                    }
                });
    }

    /**
     * The {@link MvnSettingsProvider.DescriptorImpl#doFillSettingsConfigIdItems(ItemGroup, Item, String)} is only accessible by
     * administers.
     */
    @Issue({"SECURITY-2203", "JENKINS-65436"})
    @Test
    void mvnSettingsProviderDoFillSettingsConfigIdItemsProtectedGlobalConfiguration() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(mavenSettingConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnSettingsProvider.DescriptorImpl descriptor =
                    (MvnSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(
                    Jenkins.get(), null, CURRENT); // no project, global configuration
        };
        ProtectedCodeRunner<ListBoxModel> projectChecker =
                new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

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
    void mvnSettingsProviderDoFillSettingsConfigIdItemsProtectedForProject() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(mavenSettingConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnSettingsProvider.DescriptorImpl descriptor =
                    (MvnSettingsProvider.DescriptorImpl) Jenkins.get().getDescriptorOrDie(MvnSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project, CURRENT); // we pass the project
        };
        ProtectedCodeRunner<ListBoxModel> projectChecker =
                new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

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
    void mvnGlobalSettingsProviderDoFillSettingsConfigIdItemsProtectedGlobalConfiguration() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(globalMavenSettingsConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnGlobalSettingsProvider.DescriptorImpl descriptor = (MvnGlobalSettingsProvider.DescriptorImpl)
                    Jenkins.get().getDescriptorOrDie(MvnGlobalSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(
                    Jenkins.get(), null, CURRENT); // from global configuration, no project
        };
        ProtectedCodeRunner<ListBoxModel> projectChecker =
                new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

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
    void mvnGlobalSettingsProviderDoFillSettingsConfigIdItemsProtectedForProject() {
        r.jenkins.getInjector().injectMembers(this);

        // Create a maven settings config file that will only be returned by the administer user
        Config c = createSetting(globalMavenSettingsConfigProvider);

        final String CURRENT = "current-value";

        // Code called from a project
        Supplier<ListBoxModel> settingsConfigListSupplier = () -> {
            MvnGlobalSettingsProvider.DescriptorImpl descriptor = (MvnGlobalSettingsProvider.DescriptorImpl)
                    Jenkins.get().getDescriptorOrDie(MvnGlobalSettingsProvider.class);
            return descriptor.doFillSettingsConfigIdItems(Jenkins.get(), project, CURRENT); // we pass the project
        };
        ProtectedCodeRunner<ListBoxModel> projectChecker =
                new ProtectedCodeRunner<>(settingsConfigListSupplier, "reader");

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

    private Config createSetting(ConfigProvider provider, String id) {
        Config c1;
        if (id != null) {
             c1 = provider.newConfig(id);
        } else {
             c1 = provider.newConfig();
        }
        GlobalConfigFiles globalConfigFiles =
                r.jenkins.getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        globalConfigFiles.save(c1);
        return c1;

    }
    private Config createSetting(ConfigProvider provider) {
        return createSetting(provider, null);
    }

    /**
     * The {@link ConfigFilesManagement#getTarget()} is only accessible by people able to administer jenkins. It guarantees
     * all methods in the class require {@link Jenkins#ADMINISTER}.
     */
    @Issue("SECURITY-2203")
    @Test
    void configFilesManagementAllMethodsProtected() {
        Permission permission = Jenkins.ADMINISTER;
        Runnable run = () -> {
            ConfigFilesManagement configFilesManagement =
                    Jenkins.get().getExtensionList(ConfigFilesManagement.class).get(0);
            configFilesManagement.getTarget();
        };

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName("reader"))) {
            run.run(); // The method should fail
            fail(String.format( "%s should be only accessible by people with the permission %s, but it's accessible by a person with %s",
                                        "ConfigFilesManagement#getTarget", Jenkins.ADMINISTER, Item.READ));

        } catch (AccessDeniedException e) {
            assertThat(e.getMessage(), containsString(permission.group.title + "/" + permission.name));
        }

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName("administer"))) {
            run.run(); // The method doesn't fail
        } catch (AccessDeniedException e) {
            fail(String.format(
                    "%s should be accessible to people with the permission %s but it failed with the exception: %s",
                    "ConfigFilesManagement#getTarget", Jenkins.ADMINISTER, e));
        }
    }

    @FunctionalInterface
    interface Callback<T> {
        void call(T arg);
    }

    /**
     * Common logic to check a specific method is accessible by people with Configure permission and not by any person
     * with just read permission. We don't care about the result. If you don't have permission, the method with fail.
     *
     * @param run           The method to check.
     * @param checkedMethod The name of the method for logging purposes.
     * @param failChecker   The method to call for checking the result when the user doesn't have permission.
     * @param successChecker The method to call for checking the result when the user has permission
     */
    private <T> void assertWhoCanExecute(Callable<T> run, String checkedMethod, Callback<T> failChecker, Callback<T> successChecker) {
        final Map<Permission, String> userWithPermission = Stream.of(
                        new AbstractMap.SimpleEntry<>(Jenkins.READ, "reader"),
                        new AbstractMap.SimpleEntry<>(Item.CONFIGURE, "projectConfigurer"),
                        new AbstractMap.SimpleEntry<>(Jenkins.ADMINISTER, "administer"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName("reader"))) {
            T result = run.call(); // The method should fail
            failChecker.call(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(userWithPermission.get(Item.CONFIGURE)))) {
            T result = run.call(); // The method doesn't fail
            successChecker.call(result);
        } catch (AccessDeniedException e) {
            fail(String.format(
                    "%s should be accessible to people with the permission %s but it failed with the exception: %s",
                    checkedMethod, Item.CONFIGURE, e));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
