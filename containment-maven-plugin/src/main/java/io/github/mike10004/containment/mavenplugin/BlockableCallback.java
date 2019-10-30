package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.async.ResultCallback;
import com.google.common.base.Joiner;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockableCallback<T> implements ResultCallback<T> {

    private volatile boolean started;
    private final CountDownLatch completionLatch;
    public final List<T> responseItems;
    public final List<Throwable> errors;

    public BlockableCallback() {
        this.completionLatch = new CountDownLatch(1);
        responseItems = new ArrayList<>();
        errors = new ArrayList<>();
    }

    @Override
    public void onStart(Closeable closeable) {
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public void onNext(T object) {
        responseItems.add(object);
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

    public boolean await(Duration timeout) throws InterruptedException {
        return completionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public <X extends Throwable> void completeOrThrowException(Duration timeout, Supplier<X>exceptionConstructor) throws X, InterruptedException {
        boolean completed = await(timeout);
        if (!completed) {
            throw exceptionConstructor.get();
        }
    }

    public boolean checkSucceeded(Predicate<? super T> checker) {
        List<T> responseItems = new ArrayList<>(this.responseItems); // defensive copy
        return responseItems.stream().anyMatch(checker)
                && errors.isEmpty();
    }

    public String summarize() {
        Joiner j = Joiner.on(System.lineSeparator());
        return String.format("CallbackSummary:%n%n%s%n%n%s%n",
                responseItems.isEmpty() ? "(no response items received)" : j.join(responseItems),
                errors.isEmpty() ? "(no exceptions thrown)" : j.join(errors));
    }
}
