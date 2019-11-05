package io.github.mike10004.containment.junit4;

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
import io.github.mike10004.containment.lifecycle.ContainerLifecycle;
import io.github.mike10004.containment.lifecycle.ContainerRunnerConstructor;
import io.github.mike10004.containment.lifecycle.FirstProvisionFailedException;
import io.github.mike10004.containment.lifecycle.GlobalLifecycledDependency;
import io.github.mike10004.containment.lifecycle.LifecycleEvent;
import io.github.mike10004.containment.lifecycle.LifecycledDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public interface ContainerDependency {

    RunningContainer container() throws FirstProvisionFailedException;

    static ContainerDependency of(LifecycledDependency<RunningContainer> container) {
        return () -> container.provide().require();
    }

    static Builder builder(ContainerParametry parametry) {
        return new Builder(parametry);
    }

    class Builder {

        private Consumer<? super LifecycleEvent> eventListener = ignore -> {};
        private ContainerParametry containerParametry;
        private List<ContainerAction> preStartActions;

        private Builder(ContainerParametry containerParametry) {
            this.containerParametry = requireNonNull(containerParametry, "containerParametry");
            preStartActions = new ArrayList<>();
        }

        public Builder eventListener(Consumer<? super LifecycleEvent> eventListener) {
            this.eventListener = requireNonNull(eventListener);
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

        private class LocalDependencyCreator implements Supplier<LifecycledDependency<RunningContainer>> {

            @Override
            public LifecycledDependency<RunningContainer> get() {
                return new LifecycledDependency<>(ContainerLifecycle.create(new LocalRunnerConstructor(), containerParametry, preStartActions), eventListener);
            }
        }

        private class GlobalDependencyCreator implements Supplier<GlobalLifecycledDependency<RunningContainer>> {

            @Override
            public GlobalLifecycledDependency<RunningContainer> get() {
                return new GlobalLifecycledDependency<>(ContainerLifecycle.create(new GlobalRunnerConstructor(), containerParametry, preStartActions));
            }
        }

        public ContainerDependency buildLocalDependency() {
            return buildDependency(new LocalDependencyCreator());
        }

        public ContainerDependency buildGlobalDependency() {
            return buildDependency(new GlobalDependencyCreator());
        }

        private ContainerDependency buildDependency(Supplier<? extends LifecycledDependency<RunningContainer>> dependencyCreator) {
            return ContainerDependency.of(dependencyCreator.get());
        }

        public ContainerDependencyRule buildLocalRule() {
            LifecycledDependency<RunningContainer> dependency = new LocalDependencyCreator().get();
            return new ContainerDependencyRule(dependency);
        }

    }

}
