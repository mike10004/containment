package io.github.mike10004.containment.dockerjava;

import com.google.common.base.Joiner;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a blockable callback that uses a countdown latch to await completion.
 * This was the first implementation and appears to work for our use cases,
 * but the library offers an implementation in {@link com.github.dockerjava.api.async.ResultCallbackTemplate}
 * to which we should migrate eventually.
 * @param <T>
 */
class HandrolledBlockableCallback<T> implements BlockableCallback<T> {

    private volatile boolean started;
    private final CountDownLatch completionLatch;
    public final List<T> responseItems;
    public final List<Throwable> errors;
    private final Predicate<? super T> successChecker;
    private volatile boolean succeeded;

    public HandrolledBlockableCallback(Predicate<? super T> successChecker) {
        this.completionLatch = new CountDownLatch(1);
        responseItems = new ArrayList<>();
        errors = new ArrayList<>();
        this.successChecker = requireNonNull(successChecker);
    }

    @Override
    public void onStart(Closeable closeable) {
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public final void onNext(T object) {
        responseItems.add(object);
        if (!succeeded && successChecker.test(object)) {
            succeeded = true;
        }
        received(object);
    }

    public void received(T object) {

    }

    @Override
    public void onError(Throwable throwable) {
        errors.add(throwable);
    }

    @Override
    public void onComplete() {
        completionLatch.countDown();
    }

    @Override
    public void close() {
        completionLatch.countDown();
    }

    @Override
    public boolean doAwaitCompletion(Duration timeout) throws InterruptedException {
        return completionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean checkSucceeded() {
        return succeeded;
    }

    public String summarize() {
        Joiner j = Joiner.on(System.lineSeparator());
        return String.format("CallbackSummary:%n%n%s%n%n%s%n",
                responseItems.isEmpty() ? "(no response items received)" : j.join(responseItems),
                errors.isEmpty() ? "(no exceptions thrown)" : j.join(errors));
    }
}
