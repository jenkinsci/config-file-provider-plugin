package org.jenkinsci.plugins.configfiles.sec;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.UserCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import java.io.IOException;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class Security2254Test {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    
    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    private WorkflowJob project;
    private Folder folder;
    
    @Before
    public void setUpAuthorizationAndProject() throws IOException {
        // A folder and a project inside
        folder = r.jenkins.createProject(Folder.class, "f");
        project = folder.createProject(WorkflowJob.class, "p");
        
        // The permissions
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        
        // Everyone can read
        strategy.grant(Jenkins.READ).everywhere().toEveryone();
        
        // Reader. No permissions to manage credentials
        strategy.grant(Item.READ).everywhere().to("reader");
        
        // accredited with own credentials store and able to use them in project
        strategy.grant(Item.EXTENDED_READ).onItems(project).to("accredited");
        strategy.grant(CredentialsProvider.USE_ITEM).onItems(project).to("accredited");
        
        // Project configurer on project
        strategy.grant(Item.CONFIGURE).onItems(project).to("projectConfigurer");
        
        // Folder configurer
        strategy.grant(Item.CONFIGURE).onItems(folder).to("folderConfigurer");
        
        // Administer
        strategy.grant((Jenkins.ADMINISTER)).everywhere().to("administer");
        
        r.jenkins.setAuthorizationStrategy(strategy);
        
        // A system global credential
        SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "systemCred", "", "systemUser", "systemPassword"));
        
        // The credentials in folder and for accredited
        CredentialsStore folderStore = getFolderStore(folder);
        UsernamePasswordCredentialsImpl folderCredential =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "folderCred", "description", "folderUser",
                        "folderPassword");
        folderStore.addCredentials(Domain.global(), folderCredential);

        // A credential for accredited
        CredentialsStore userStore = getUserStore(User.getOrCreateByIdOrFullName("accredited"));
        UsernamePasswordCredentialsImpl userCredential =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "userCred", "description", "accreditedUser",
                        "accreditedPassword");
        // SYSTEM cannot add credentials to the user store
        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName("accredited"))) {
            userStore.addCredentials(Domain.global(), userCredential);
        }
    }
    
    @Test
    @Issue("SECURITY-2254")
    public void fillCredentialIdItemsForServer() throws Exception {
        final String CURRENT = "current-value";
        
        // Code called from global pages
        Supplier<ListBoxModel> systemCredentialListSupplier = () -> {
            ServerCredentialMapping.DescriptorImpl descriptor = (ServerCredentialMapping.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ServerCredentialMapping.class);
            return descriptor.doFillCredentialsIdItems(r.jenkins, null, CURRENT);
        };
        ProtectedCodeRunner<ListBoxModel> globalChecker = new ProtectedCodeRunner<>(systemCredentialListSupplier, "user-will-be-replaced");

        // Code called from a project
        Supplier<ListBoxModel> projectCredentialListSupplier = () -> {
            ServerCredentialMapping.DescriptorImpl descriptor = (ServerCredentialMapping.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ServerCredentialMapping.class);
            return descriptor.doFillCredentialsIdItems(folder, project, CURRENT);
        };
        ProtectedCodeRunner<ListBoxModel> projectChecker = new ProtectedCodeRunner<>(projectCredentialListSupplier, "user-will-be-replaced");
        
        // Code called from a folder
        Supplier<ListBoxModel> folderCredentialListSupplier = () -> {
            ServerCredentialMapping.DescriptorImpl descriptor = (ServerCredentialMapping.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ServerCredentialMapping.class);
            return descriptor.doFillCredentialsIdItems(r.jenkins, folder, CURRENT);
        };
        ProtectedCodeRunner<ListBoxModel> folderChecker = new ProtectedCodeRunner<>(folderCredentialListSupplier, "user-will-be-replaced");

        ListBoxModel result;
        
        // Reader doesn't get the list of credentials
        result = globalChecker.withUser("reader").getResult();
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value, equalTo(CURRENT));

        // Administer has access to the one stored
        result = globalChecker.withUser("administer").getResult();
        assertThat(result, hasSize(2));
        assertThat(result.get(1).value, equalTo("systemCred"));

        // accredited see system global and folder ones. Their own credentials are not available because the 
        // gathering of the credentials is used by ACL.SYSTEM. See: https://github.com/jenkinsci/config-file-provider-plugin/blob/master/src/main/java/org/jenkinsci/plugins/configfiles/maven/security/ServerCredentialMapping.java#L64
        // So there is no visibility of their own credentials while configuring the project.
        result = projectChecker.withUser("accredited").getResult();
        assertThat(result, hasSize(3));
        assertThat(result.get(1).value, equalTo("folderCred")); // system, folder
        assertThat(result.get(2).value, equalTo("systemCred")); // project

        // Project configurer see system and folder ones because CONFIGURE implies USE_ITEMS of credentials
        result = projectChecker.withUser("projectConfigurer").getResult();
        assertThat(result, hasSize(3));
        assertThat(result.get(1).value, equalTo("folderCred")); // system, folder
        assertThat(result.get(2).value, equalTo("systemCred")); // project
        
        // Folder configurer, without access to the project cannot get them
        result = globalChecker.withUser("folderConfigurer").getResult();
        assertThat(result, hasSize(1));
        assertThat(result.get(0).value, equalTo(CURRENT));
    }
    
    private CredentialsStore getFolderStore(Folder f) {
        return getCredentialStore(f, FolderCredentialsProvider.class);
    }

    private CredentialsStore getUserStore(User u) {
        return getCredentialStore(u, UserCredentialsProvider.class);
    }
    
    private CredentialsStore getCredentialStore(ModelObject object, Class<? extends CredentialsProvider> clazz) {
        Iterable<CredentialsStore> stores = CredentialsProvider.lookupStores(object);
        CredentialsStore folderStore = null;
        for (CredentialsStore s : stores) {
            if (clazz.isInstance(s.getProvider()) && s.getContext() == object) {
                folderStore = s;
                break;
            }
        }
        return folderStore;
    }
    
}
