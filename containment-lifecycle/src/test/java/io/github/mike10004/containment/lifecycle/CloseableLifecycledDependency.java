package io.github.mike10004.containment.lifecycle;

public class CloseableLifecycledDependency<T> extends LifecycledDependency<T> implements AutoCloseable {

    public CloseableLifecycledDependency(DependencyLifecycle<T> lifecycle) {
        super(lifecycle);
    }

    @Override
    public void close() {
        finishLifecycle();
    }
}
