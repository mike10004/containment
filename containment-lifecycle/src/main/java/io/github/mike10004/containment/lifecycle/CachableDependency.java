package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

class CacheableProgressiveDependency<T> implements ProgressiveDependency<T> {

    private final LifecyclingCachingProvider<T> containerProvider;

    public CacheableProgressiveDependency(LifecyclingCachingProvider<T> containerProvider) {
        this.containerProvider = requireNonNull(containerProvider, "containerProvider");
    }

    @Override
    public T container() throws FirstProvisionFailedException {
        return containerProvider.provide().require();
    }

    @Override
    public void finishLifecycle() {
        containerProvider.finishLifecycle();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("containerProvider=" + containerProvider)
                .toString();
    }
}

class CachableDependency extends CacheableProgressiveDependency<StartedContainer> implements ContainerDependency {

    public CachableDependency(LifecyclingCachingProvider<StartedContainer> containerProvider) {
        super(containerProvider);
    }
}
