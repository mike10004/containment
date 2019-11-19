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
     * @return a new resource instance
     */
    public <T> LifecycledResource<T> buildResource(Lifecycle<T> stack) {
        return buildResourceFromProvider(new LifecyclingCachingProvider<>(stack, eventListener));
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
     * @return a new resource instance
     */
    public <T> LifecycledResource<T> buildResourceDecommissionedOnJvmTermination(Lifecycle<T> stack) {
        return buildResourceFromProvider(new GlobalLifecyclingCachingProvider<>(stack, eventListener));
    }

    /**
     * @deprecated use {@link #buildResourceDecommissionedOnJvmTermination(Lifecycle)} for clarity
     */
    @Deprecated
    public <T> LifecycledResource<T> buildGlobalResource(Lifecycle<T> stack) {
        return buildResourceDecommissionedOnJvmTermination(stack);
    }

    private <T> LifecycledResource<T> buildResourceFromProvider(LifecyclingCachingProvider<T> provider) {
        return LifecycledResource.fromProvider(provider);
    }

}
