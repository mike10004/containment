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
     * Builds a new resource instance. Resource instances
     * must have their {@link LifecycledResource#finishLifecycle()} invoked
     * explicitly to decommission the resource (if it has been commissioned).
     * @param lifecycle lifecycle to manage
     * @return a new resource instance
     * @param <T> type of resource the lifecycle produces
     */
    public <T> LifecycledResource<T> buildResource(Lifecycle<T> lifecycle) {
        return buildResourceFromProvider(new LifecyclingCachingProvider<>(lifecycle, eventListener));
    }

    /**
     * Builds a resource instance that is decommissioned on JVM termination.
     * The resource is decommissioned <i>only</i> on JVM termination.
     * The resources's {@link LifecycledResource#finishLifecycle()}
     * method is effectively disabled and does not actually decommission the resource.
     * (If you want to decommission explicitly, you could retain a reference
     * to the argument lifecycle and invoke its {@link Lifecycle#decommission()} method,
     * but this is not recommended, because decommissioning will then occur <i>again</i>
     * on JVM termination. So, only do so if you know that double-decommissioning
     * does not have any nasty side effects.)
     * @param lifecycle lifecycle to manage
     * @return a new resource instance
     * @param <T> type of resource the lifecycle produces
     */
    public <T> LifecycledResource<T> buildResourceDecommissionedOnJvmTermination(Lifecycle<T> lifecycle) {
        return buildResourceFromProvider(new GlobalLifecyclingCachingProvider<>(lifecycle, eventListener));
    }

    private <T> LifecycledResource<T> buildResourceFromProvider(LifecyclingCachingProvider<T> provider) {
        return LifecycledResource.fromProvider(provider);
    }

}
