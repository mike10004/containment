package io.github.mike10004.containment.lifecycle;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Builder of lifecycled resources.
 */
public class LifecycledResourceBuilder {

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();

    protected LifecycledResourceBuilder() {
    }

    /**
     * Sets the lifecycle event listener.
     * @param eventListener listener
     * @return this builder instance
     */
    public LifecycledResourceBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    /**
     * Builds a new local resource instance. Local resource instances
     * must have their {@link ContainerResource#finishLifecycle()} invoked
     * explicitly to decommission the resource (if it has been commissioned).
     * @return a new resource instance
     */
    public <T> LifecycledResource<T> buildLocalResource(Lifecycle<T> stack) {
        return buildResourceFromProvider(new LifecyclingCachingProvider<>(stack, eventListener));
    }

    /**
     * Builds a global resource instance. The instance's {@link LifecycledResource#finishLifecycle()}
     * method does not actually decommission the resource; instead, decommissioning occurs
     * at time of JVM termination.
     * @return a new resource instance
     */
    public <T> LifecycledResource<T> buildGlobalResource(Lifecycle<T> stack) {
        return buildResourceFromProvider(new GlobalLifecyclingCachingProvider<>(stack, eventListener));
    }

    private <T> LifecycledResource<T> buildResourceFromProvider(LifecyclingCachingProvider<T> provider) {
        return LifecycledResource.fromProvider(provider);
    }

}
