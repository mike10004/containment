package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service that lazily provides a running container.
 */
public interface ContainerDependency {

    /**
     * Returns an already-started container or starts and returns
     * a not-yet-started container.
     * @return a started container
     * @throws FirstProvisionFailedException if provisioning the container fails
     */
    StartedContainer container() throws FirstProvisionFailedException;

    /**
     * Stops and removes the container, if it has been started.
     */
    void finishLifecycle();

    /**
     * Creates an instance from a container provider.
     * @param containerProvider the provider
     * @return a new instance
     */
    static ContainerDependency fromProvider(LifecyclingCachingProvider<StartedContainer> containerProvider) {
        return new ProviderDependency(containerProvider);
    }

    /**
     * Creates a new builder.
     * @param parametry container parameters
     * @return a new builder instance
     */
    static Builder builder(ContainerParametry parametry) {
        return new Builder(parametry);
    }

    /**
     * Builder of container dependency instances.
     */
    class Builder {

        private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();
        private final ContainerLifecycles.Builder lifecycleBuilder;
        private final Function<? super DjDockerManager, ? extends ContainerCreator> djCreatorConstructor;

        protected Builder(ContainerParametry containerParametry) {
            this(containerParametry, DjContainerCreator::new);
        }

        protected Builder(ContainerParametry containerParametry, Function<? super DjDockerManager, ? extends ContainerCreator> djCreatorConstructor) {
            this.lifecycleBuilder = ContainerLifecycles.builder(containerParametry);
            this.djCreatorConstructor = requireNonNull(djCreatorConstructor);
        }

        /**
         * Sets the container lifecycle event listener.
         * @param eventListener listener
         * @return this builder instance
         */
        public Builder eventListener(Consumer<? super LifecycleEvent> eventListener) {
            this.eventListener = requireNonNull(eventListener);
            return this;
        }

        /**
         * Adds an action that is to be executed after starting the container.
         * @param containerAction container action
         * @return this builder instance
         */
        public Builder addPostStartAction(StartedContainerAction containerAction) {
            lifecycleBuilder.postStart(containerAction);
            return this;
        }

        /**
         * Adds an action that is to be executed
         * after creating and prior to starting the container.
         * @param action the action
         * @return this builder instance
         */
        public Builder addPreStartAction(ContainerAction action) {
            lifecycleBuilder.preStart(action);
            return this;
        }

        /**
         * Constructor of creators for global containers. Cleanup of these containers is
         * managed by a {@link GlobalLifecyclingCachingProvider}, so they use
         * manual container monitors.
         */
        private static class GlobalContainerCreatorConstructor implements ContainerCreatorConstructor {

            private final Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor;

            public GlobalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
                this.djConstructor = djConstructor;
            }

            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjManualContainerMonitor());
                return djConstructor.apply(manager);
            }
        }

        /**
         * Constructor of creators for local containers. Cleanup of these containers is managed
         * by the caller, but a global monitor adds a shutdown hook to clean up ones the caller forgot.
         */
        private static class LocalContainerCreatorConstructor implements ContainerCreatorConstructor {

            private final Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor;

            public LocalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
                this.djConstructor = djConstructor;
            }

            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build()));
                return djConstructor.apply(manager);
            }
        }

        /**
         * Builds a new local dependency instance. Local dependency instances
         * must have their {@link ContainerDependency#finishLifecycle()} invoked
         * explicitly to stop and remove the container, if it has been started.
         * @return a new instance
         */
        public ContainerDependency buildLocalDependency() {
            return buildDependencyFromProvider(new LifecyclingCachingProvider<>(lifecycleBuilder.build(new LocalContainerCreatorConstructor(djCreatorConstructor)), eventListener));
        }

        /**
         * Builds a global dependency instance. The instance's {@link ContainerDependency#finishLifecycle()}
         * method will not actually cause the end of the container's lifecycle to be executed; that will
         * only happen upon JVM termination.
         * @return a new instance
         */
        public ContainerDependency buildGlobalDependency() {
            return buildDependencyFromProvider(new GlobalLifecyclingCachingProvider<>(lifecycleBuilder.build(new GlobalContainerCreatorConstructor(djCreatorConstructor)), eventListener));
        }

        private ContainerDependency buildDependencyFromProvider(LifecyclingCachingProvider<StartedContainer> provider) {
            return ContainerDependency.fromProvider(provider);
        }

    }

}
