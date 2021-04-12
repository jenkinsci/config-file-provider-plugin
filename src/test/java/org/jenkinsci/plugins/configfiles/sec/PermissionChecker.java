package org.jenkinsci.plugins.configfiles.sec;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;
import org.hamcrest.core.IsEqual;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * A class to run pieces of code with a certain user and assert either the code runs successfully, without even worrying
 * about the result, or the code fails with an {@link AccessDeniedException2} with the specified {@link Permission}.
 */
public class PermissionChecker extends ProtectedCodeRunner<Void> {
    /**
     * Build a checker to run a specific code with this user and assert it runs successfully or it fails because certain
     * permission.
     * @param code The code to execute.
     * @param user The user to execute with.
     */
    public PermissionChecker(@NonNull Runnable code, @NonNull String user) {
        super(getSupplier(code), user);
    }

    /**
     * Assert the execution of the code by this user fails with this permission. The code throws an {@link AccessDeniedException2}
     * with the permission field being permission. Otherwise it fails.
     * @param permission The permission thrown by the code.
     */
    public void assertFailWithPermission(Permission permission) {
        Throwable t = getThrowable();
        if (t instanceof AccessDeniedException2) {
            assertThat(((AccessDeniedException2) t).permission, IsEqual.equalTo(permission));
        } else {
            fail(String.format("The code run by %s didn't throw an AccessDeniedException2 with %s. If failed with the unexpected throwable: %s", getUser(), permission, t));
        }
    }

    /**
     * Assert the execution is done without any exception. The result doesn't matter.
     */
    public void assertPass() {
        getResult(); // The result doesn't matter
    }

    /**
     * Change the user to run the code with.
     * @param user The user.
     * @return This object.
     */
    @Override
    public PermissionChecker withUser(String user) {
        super.withUser(user);
        return this;
    }

    /**
     * Get a supplier from a runnable.
     * @param code The runnable to run.
     * @return A supplier executing the runnable code and returning just null.
     */
    private static Supplier<Void> getSupplier(Runnable code) {
        return () -> {
            code.run();
            return null;
        };
    }
}
