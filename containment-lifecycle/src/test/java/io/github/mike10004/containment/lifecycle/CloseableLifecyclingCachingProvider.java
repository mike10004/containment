package io.github.mike10004.containment.lifecycle;

public class CloseableLifecyclingCachingProvider<T> extends LifecyclingCachingProvider<T> implements AutoCloseable {

    public CloseableLifecyclingCachingProvider(Lifecycle<T> lifecycle) {
        super(lifecycle);
    }

    @Override
    public void close() {
        finishLifecycle();
    }
}
