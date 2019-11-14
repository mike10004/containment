package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Builder of container dependency instances.
 */
public class ContainerDependencyBuilder {

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();
    private final ContainerLifecycles.Builder lifecycleBuilder;
    private final Function<? super DjDockerManager, ? extends ContainerCreator> djCreatorConstructor;

    protected ContainerDependencyBuilder(ContainerParametry containerParametry) {
        this(containerParametry, DjContainerCreator::new);
    }

    protected ContainerDependencyBuilder(ContainerParametry containerParametry, Function<? super DjDockerManager, ? extends ContainerCreator> djCreatorConstructor) {
        this.lifecycleBuilder = ContainerLifecycles.builder(containerParametry);
        this.djCreatorConstructor = requireNonNull(djCreatorConstructor);
    }

    /**
     * Sets the container lifecycle event listener.
     * @param eventListener listener
     * @return this builder instance
     */
    public ContainerDependencyBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    /**
     * Adds an action that is to be executed after starting the container.
     * @param containerAction container action
     * @return this builder instance
     */
    public ContainerDependencyBuilder addPostStartAction(StartedContainerAction containerAction) {
        lifecycleBuilder.postStart(containerAction);
        return this;
    }

    /**
     * Adds an action that is to be executed
     * after creating and prior to starting the container.
     * @param action the action
     * @return this builder instance
     */
    public ContainerDependencyBuilder addPreStartAction(ContainerAction action) {
        lifecycleBuilder.preStart(action);
        return this;
    }

    /**
     * Constructor of creators for global containers. Cleanup of these containers is
     * managed by a {@link GlobalLifecyclingCachingProvider}, so they use
     * manual container monitors.
     */
    private class BuilderGlobalContainerCreatorConstructor extends GlobalContainerCreatorConstructor {

        public BuilderGlobalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
            super(djConstructor, ContainerDependencyBuilder.this::createManualContainerMonitor);
        }

    }

    /**
     * Constructor of creators for local containers. Cleanup of these containers is managed
     * by the caller, but a global monitor adds a shutdown hook to clean up ones the caller forgot.
     */
    private class BuilderLocalContainerCreatorConstructor extends LocalContainerCreatorConstructor {

        public BuilderLocalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
            super(djConstructor, ContainerDependencyBuilder.this::createShutdownHookContainerMonitor);
        }
    }

    protected DjContainerMonitor createManualContainerMonitor(DockerClientConfig config) {
        return new DjManualContainerMonitor();
    }

    protected DjContainerMonitor createShutdownHookContainerMonitor(DockerClientConfig config) {
        return new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build());
    }

    /**
     * Builds a new local dependency instance. Local dependency instances
     * must have their {@link ContainerDependency#finishLifecycle()} invoked
     * explicitly to stop and remove the container, if it has been started.
     * @return a new instance
     */
    public ContainerDependency buildLocalDependency() {
        return buildDependencyFromProvider(new LifecyclingCachingProvider<>(lifecycleBuilder.build(new BuilderLocalContainerCreatorConstructor(djCreatorConstructor)), eventListener));
    }

    /**
     * Builds a global dependency instance. The instance's {@link ContainerDependency#finishLifecycle()}
     * method will not actually cause the end of the container's lifecycle to be executed; that will
     * only happen upon JVM termination.
     * @return a new instance
     */
    public ContainerDependency buildGlobalDependency() {
        return buildDependencyFromProvider(new GlobalLifecyclingCachingProvider<>(lifecycleBuilder.build(new BuilderGlobalContainerCreatorConstructor(djCreatorConstructor)), eventListener));
    }

    private ContainerDependency buildDependencyFromProvider(LifecyclingCachingProvider<StartedContainer> provider) {
        return ContainerDependency.fromProvider(provider);
    }

}

