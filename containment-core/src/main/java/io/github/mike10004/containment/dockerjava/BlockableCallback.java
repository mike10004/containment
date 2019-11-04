package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.async.ResultCallback;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public interface BlockableCallback<T> extends ResultCallback<T> {

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
