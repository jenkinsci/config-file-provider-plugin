package org.jenkinsci.plugins.configfiles.sec;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;

import java.util.function.Supplier;

import static org.junit.Assert.fail;

/**
 * Class to run a code returning something with a specific user. You can get the result or the {@link Throwable} thrown
 * by the code executed. Afterwards you can assert the result or the exception thrown.
 * @param <Result>
 */
public class ProtectedCodeRunner<Result> {
    @NonNull
    private final Supplier<Result> code;
    @NonNull
    private String user;

    /**
     * Create an object to run this piece of code with this Jenkins user.
     * @param code The code to be executed.
     * @param user The user executing this code.
     */
    public ProtectedCodeRunner(@NonNull Supplier<Result> code, @NonNull String user) {
        this.code = code;
        this.user = user;
    }

    /**
     * We run the code expecting to get a result. If the execution throws an exception (Throwable), the test fails with
     * a descriptive message.
     * @return The result of the execution.
     */
    public Result getResult() {
        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(user))) {
            return code.get();
        } catch (Throwable t) {
            fail(String.format("The code executed by %s didn't run successfully. The throwable thrown is: %s", user, t));
            return null;
        }
    }

    /**
     * We run the code expecting an exception to be thrown. If it's not the case, the test fails with a descriptive 
     * message.
     * @return The {@link Throwable} thrown by the execution.
     */
    public Throwable getThrowable() {
        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(user))) {
            Result result = code.get();
            fail(String.format("The code executed by %s was successful but we were expecting it to fail. The result of the execution was: %s", user, result));
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    /**
     * Get the user.
     * @return The user.
     */
    public String getUser() {
        return user;
    }

    /**
     * Use a different user.
     * @param user The user.
     * @return This object.
     */
    public ProtectedCodeRunner<Result> withUser(String user) {
        this.user = user;
        return this;
    }
}
