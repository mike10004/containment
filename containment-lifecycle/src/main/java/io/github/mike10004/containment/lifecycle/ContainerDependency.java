package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerAction;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        private ContainerParametry containerParametry;
        private List<ContainerAction> preStartActions;
        private List<StartedContainerAction> postStartActions;

        private Builder(ContainerParametry containerParametry) {
            this.containerParametry = requireNonNull(containerParametry, "containerParametry");
            preStartActions = new ArrayList<>();
            postStartActions = new ArrayList<>();
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
            postStartActions.add(containerAction);
            return this;
        }

        /**
         * Adds an action that is to be executed
         * after creating and prior to starting the container.
         * @param action the action
         * @return this builder instance
         */
        public Builder addPreStartAction(ContainerAction action) {
            preStartActions.add(action);
            return this;
        }

        private static class GlobalRunnerConstructor implements ContainerCreatorConstructor {

            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjManualContainerMonitor());
                return new DjContainerCreator(manager);
            }
        }

        private static class LocalRunnerConstructor implements ContainerCreatorConstructor {
            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build()));
                return new DjContainerCreator(manager);
            }
        }

        private class LocalProviderCreator implements Supplier<LifecyclingCachingProvider<StartedContainer>> {

            @Override
            public LifecyclingCachingProvider<StartedContainer> get() {
                return new LifecyclingCachingProvider<>(ContainerLifecycle.create(new LocalRunnerConstructor(), containerParametry, preStartActions, postStartActions), eventListener);
            }
        }

        private class GlobalProviderCreator implements Supplier<GlobalLifecyclingCachingProvider<StartedContainer>> {

            @Override
            public GlobalLifecyclingCachingProvider<StartedContainer> get() {
                return new GlobalLifecyclingCachingProvider<>(ContainerLifecycle.create(new GlobalRunnerConstructor(), containerParametry, preStartActions, postStartActions));
            }
        }

        /**
         * Builds a new local dependency instance. Local dependency instances
         * must have their {@link ContainerDependency#finishLifecycle()} invoked
         * explicitly to stop and remove the container, if it has been started.
         * @return a new instance
         */
        public ContainerDependency buildLocalDependency() {
            return buildDependencyFromProviderCreator(new LocalProviderCreator());
        }

        /**
         * Builds a global dependency instance. The instance's {@link ContainerDependency#finishLifecycle()}
         * method will not actually cause the end of the container's lifecycle to be executed; that will
         * only happen upon JVM termination.
         * @return a new instance
         */
        public ContainerDependency buildGlobalDependency() {
            return buildDependencyFromProviderCreator(new GlobalProviderCreator());
        }

        private ContainerDependency buildDependencyFromProviderCreator(Supplier<? extends LifecyclingCachingProvider<StartedContainer>> dependencyCreator) {
            return ContainerDependency.fromProvider(dependencyCreator.get());
        }

    }

}
