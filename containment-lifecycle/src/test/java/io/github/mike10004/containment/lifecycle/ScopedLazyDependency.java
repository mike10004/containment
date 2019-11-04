package io.github.mike10004.containment.lifecycle;

public class ScopedLazyDependency<T> extends LifecycledDependency<T> implements AutoCloseable {

    public ScopedLazyDependency(DependencyLifecycle<T> lifecycle) {
        super(lifecycle);
    }

    @Override
    public void close() {
        finishLifecycle();
    }
}
