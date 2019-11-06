package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.async.ResultCallback;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a callback for async commands that allows the caller to block
 * on the current thread until the command completes.
 * @param <T> the callback result type
 */
interface BlockableCallback<T> extends ResultCallback<T> {

    boolean doAwaitCompletion(Duration timeout) throws InterruptedException;

    default <X extends Throwable> void completeOrThrowException(Duration timeout, Supplier<X>exceptionConstructor) throws X, InterruptedException {
        boolean completed = doAwaitCompletion(timeout);
        if (!completed) {
            throw exceptionConstructor.get();
        }
    }

    boolean checkSucceeded();

    String summarize();

    static <T> BlockableCallback<T> createSuccessCheckingCallback(Predicate<? super T> successChecker) {
        return new HandrolledBlockableCallback<>(successChecker);
    }
}

