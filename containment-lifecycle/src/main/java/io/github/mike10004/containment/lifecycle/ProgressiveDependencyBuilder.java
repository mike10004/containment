package io.github.mike10004.containment.lifecycle;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class ProgressiveDependencyBuilder {

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();

    protected ProgressiveDependencyBuilder() {
    }

    public ProgressiveDependencyBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    /**
     * Builds a new local dependency instance. Local dependency instances
     * must have their {@link ContainerResource#finishLifecycle()} invoked
     * explicitly to stop and remove the container, if it has been started.
     * @return a new instance
     */
    public <T> ProgressiveResource<T> buildLocalDependency(ProgressiveLifecycleStack<T> stack) {
        return buildDependencyFromProvider(new LifecyclingCachingProvider<>(stack, eventListener));
    }

    /**
     * Builds a global dependency instance. The instance's {@link ContainerResource#finishLifecycle()}
     * method will not actually cause the end of the container's lifecycle to be executed; that will
     * only happen upon JVM termination.
     * @return a new instance
     */
    public <T> ProgressiveResource<T> buildGlobalDependency(ProgressiveLifecycleStack<T> stack) {
        return buildDependencyFromProvider(new GlobalLifecyclingCachingProvider<>(stack, eventListener));
    }

    private <T> ProgressiveResource<T> buildDependencyFromProvider(LifecyclingCachingProvider<T> provider) {
        return ProgressiveResource.fromProvider(provider);
    }

}
