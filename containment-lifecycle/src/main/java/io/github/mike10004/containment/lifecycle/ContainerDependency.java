package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.RunningContainer;
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

    RunningContainer container() throws FirstProvisionFailedException;

    void finishLifecycle();

    static ContainerDependency fromProvider(LifecyclingCachingProvider<RunningContainer> containerProvider) {
        return new ProviderDependency(containerProvider);
    }

    static Builder builder(ContainerParametry parametry) {
        return new Builder(parametry);
    }

    class Builder {

        private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();
        private ContainerParametry containerParametry;
        private List<ContainerAction> preStartActions;
        private List<RunningContainerAction> postStartActions;

        private Builder(ContainerParametry containerParametry) {
            this.containerParametry = requireNonNull(containerParametry, "containerParametry");
            preStartActions = new ArrayList<>();
            postStartActions = new ArrayList<>();
        }

        public Builder eventListener(Consumer<? super LifecycleEvent> eventListener) {
            this.eventListener = requireNonNull(eventListener);
            return this;
        }

        public Builder addPostStartAction(RunningContainerAction containerAction) {
            postStartActions.add(containerAction);
            return this;
        }

        public Builder addPreStartAction(ContainerAction action) {
            preStartActions.add(action);
            return this;
        }

        private static class GlobalRunnerConstructor implements ContainerRunnerConstructor {

            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjManualContainerMonitor());
                return new DjContainerCreator(manager);
            }
        }

        private static class LocalRunnerConstructor implements ContainerRunnerConstructor {
            @Override
            public ContainerCreator instantiate() throws ContainmentException {
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                DjDockerManager manager = new DefaultDjDockerManager(config, new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build()));
                return new DjContainerCreator(manager);
            }
        }

        private class LocalProviderCreator implements Supplier<LifecyclingCachingProvider<RunningContainer>> {

            @Override
            public LifecyclingCachingProvider<RunningContainer> get() {
                return new LifecyclingCachingProvider<>(ContainerLifecycle.create(new LocalRunnerConstructor(), containerParametry, preStartActions, postStartActions), eventListener);
            }
        }

        private class GlobalProviderCreator implements Supplier<GlobalLifecyclingCachingProvider<RunningContainer>> {

            @Override
            public GlobalLifecyclingCachingProvider<RunningContainer> get() {
                return new GlobalLifecyclingCachingProvider<>(ContainerLifecycle.create(new GlobalRunnerConstructor(), containerParametry, preStartActions, postStartActions));
            }
        }

        public ContainerDependency buildLocalDependency() {
            return buildDependencyFromProviderCreator(new LocalProviderCreator());
        }

        /**
         * Builds a global dependency instance. The instance's {@link ContainerDependency#finishLifecycle()}
         * method will not actually cause the end of the container's lifecycle to be executed; that will
         * only happen upon JVM termination.
         * @return
         */
        public ContainerDependency buildGlobalDependency() {
            return buildDependencyFromProviderCreator(new GlobalProviderCreator());
        }

        private ContainerDependency buildDependencyFromProviderCreator(Supplier<? extends LifecyclingCachingProvider<RunningContainer>> dependencyCreator) {
            return ContainerDependency.fromProvider(dependencyCreator.get());
        }

    }

}
