package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

class CachableDependency implements ContainerDependency {

    private final LifecyclingCachingProvider<StartedContainer> containerProvider;

    public CachableDependency(LifecyclingCachingProvider<StartedContainer> containerProvider) {
        this.containerProvider = requireNonNull(containerProvider, "containerProvider");
    }

    @Override
    public StartedContainer container() throws FirstProvisionFailedException {
        return containerProvider.provide().require();
    }

    @Override
    public void finishLifecycle() {
        containerProvider.finishLifecycle();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CachableDependency.class.getSimpleName() + "[", "]")
                .add("containerProvider=" + containerProvider)
                .toString();
    }
}
