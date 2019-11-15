package io.github.mike10004.containment.lifecycle;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class ProgressiveResourceBuilder {

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();

    protected ProgressiveResourceBuilder() {
    }

    public ProgressiveResourceBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    /**
     * Builds a new local resource instance. Local resource instances
     * must have their {@link ContainerResource#finishLifecycle()} invoked
     * explicitly to stop and remove the container, if it has been started.
     * @return a new instance
     */
    public <T> ProgressiveResource<T> buildLocalResource(ProgressiveLifecycleStack<T> stack) {
        return buildResourceFromProvider(new LifecyclingCachingProvider<>(stack, eventListener));
    }

    /**
     * Builds a global resource instance. The instance's {@link ContainerResource#finishLifecycle()}
     * method will not actually cause the end of the container's lifecycle to be executed; that will
     * only happen upon JVM termination.
     * @return a new instance
     */
    public <T> ProgressiveResource<T> buildGlobalResource(ProgressiveLifecycleStack<T> stack) {
        return buildResourceFromProvider(new GlobalLifecyclingCachingProvider<>(stack, eventListener));
    }

    private <T> ProgressiveResource<T> buildResourceFromProvider(LifecyclingCachingProvider<T> provider) {
        return ProgressiveResource.fromProvider(provider);
    }

}
