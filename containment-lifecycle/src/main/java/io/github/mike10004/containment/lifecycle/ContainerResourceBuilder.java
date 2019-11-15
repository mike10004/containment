package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Builder of container dependency instances.
 */
public abstract class ContainerResourceBuilder {

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();
    private final ContainerLifecycles.Builder lifecycleBuilder;

    protected ContainerResourceBuilder(ContainerParametry containerParametry) {
        this.lifecycleBuilder = ContainerLifecycles.builder(containerParametry);
    }

    /**
     * Sets the container lifecycle event listener.
     * @param eventListener listener
     * @return this builder instance
     */
    public ContainerResourceBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    /**
     * Adds an action that is to be executed after starting the container.
     * @param containerAction container action
     * @return this builder instance
     */
    public ContainerResourceBuilder addPostStartAction(StartedContainerAction containerAction) {
        lifecycleBuilder.postStart(containerAction);
        return this;
    }

    /**
     * Adds an action that is to be executed
     * after creating and prior to starting the container.
     * @param action the action
     * @return this builder instance
     */
    public ContainerResourceBuilder addPreStartAction(ContainerAction action) {
        lifecycleBuilder.preStart(action);
        return this;
    }

    /**
     * Builds a new local dependency instance. Local dependency instances
     * must have their {@link ContainerResource#finishLifecycle()} invoked
     * explicitly to stop and remove the container, if it has been started.
     * @return a new instance
     */
    public ContainerResource buildLocalResource() {
        return buildResourceFromProvider(new LifecyclingCachingProvider<>(lifecycleBuilder.build(buildLocalContainerCreatorConstructor()), eventListener));
    }

    /**
     * Builds a global dependency instance. The instance's {@link ContainerResource#finishLifecycle()}
     * method will not actually cause the end of the container's lifecycle to be executed; that will
     * only happen upon JVM termination.
     * @return a new instance
     */
    public ContainerResource buildGlobalResource() {

        return buildResourceFromProvider(new GlobalLifecyclingCachingProvider<>(
                lifecycleBuilder.build(
                        buildGlobalContainerCreatorConstructor()), eventListener));
    }

    protected abstract ContainerCreatorConstructor buildGlobalContainerCreatorConstructor();

    protected abstract ContainerCreatorConstructor buildLocalContainerCreatorConstructor();

    private ContainerResource buildResourceFromProvider(LifecyclingCachingProvider<StartedContainer> provider) {
        return ContainerResource.fromProvider(provider);
    }

}

