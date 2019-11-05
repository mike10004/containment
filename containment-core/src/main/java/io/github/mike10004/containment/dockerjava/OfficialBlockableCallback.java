package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.async.ResultCallback;
import com.google.common.base.MoreObjects;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Implementation of a blockable callback that uses the
 * {@code docker-java} blocking implementation. Invokes the
 * superclass {@link com.github.dockerjava.api.async.ResultCallbackTemplate#awaitCompletion(long, TimeUnit)}
 * method. We should migrate to this eventually.
 * @param <T>
 */
class OfficialBlockableCallback<T> extends com.github.dockerjava.api.async.ResultCallbackTemplate<ResultCallback<T>, T> implements BlockableCallback<T> {

    private final Predicate<? super T> successChecker;
    private volatile boolean succeeded;

    OfficialBlockableCallback(Predicate<? super T> successChecker) {
        this.successChecker = successChecker;
    }

    @Override
    public final void onNext(T object) {
        if (!succeeded && successChecker.test(object)) {
            succeeded = true;
        }
        received(object);
    }

    public void received(T object) {

    }

    @Override
    public boolean doAwaitCompletion(Duration timeout) throws InterruptedException {
        return awaitCompletion(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean checkSucceeded() {
        return succeeded;
    }

    @Override
    public String summarize() {
        return MoreObjects.toStringHelper(this).add("succeeded", succeeded).toString();
    }
}
