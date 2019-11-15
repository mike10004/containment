package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

class CacheableProgressiveResource<T> implements ProgressiveResource<T> {

    private final LifecyclingCachingProvider<T> containerProvider;

    public CacheableProgressiveResource(LifecyclingCachingProvider<T> containerProvider) {
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
