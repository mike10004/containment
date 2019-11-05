package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.RunningContainer;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

class ProviderDependency implements ContainerDependency {

    private final LifecyclingCachingProvider<RunningContainer> containerProvider;

    public ProviderDependency(LifecyclingCachingProvider<RunningContainer> containerProvider) {
        this.containerProvider = requireNonNull(containerProvider, "containerProvider");
    }

    @Override
    public RunningContainer container() throws FirstProvisionFailedException {
        return containerProvider.provide().require();
    }

    @Override
    public void finishLifecycle() {
        containerProvider.finishLifecycle();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProviderDependency.class.getSimpleName() + "[", "]")
                .add("containerProvider=" + containerProvider)
                .toString();
    }
}
