package io.github.mike10004.containment.junit4;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.lifecycle.ContainerLifecycle;
import io.github.mike10004.containment.lifecycle.ContainerRunnerConstructor;
import io.github.mike10004.containment.lifecycle.GlobalLifecycledDependency;
import io.github.mike10004.containment.lifecycle.LifecycledDependency;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ContainerDependencyRule extends ExternalResource {

    private final RuleDelegate wrappedRule;

    protected ContainerDependencyRule(RuleDelegate wrappedRule) {
        this.wrappedRule = wrappedRule;
    }

    @Override
    protected void before() throws Throwable {
        wrappedRule.before();
    }

    @Override
    protected void after() {
        wrappedRule.after();
    }

    public RunningContainer container() {
        return wrappedRule.container();
    }

    public static class Builder {

        private Consumer<? super String> eventListener = ignore -> {};
        private ContainerParametry containerParametry;
        private List<ContainerAction> preStartActions;
        private Supplier<? extends LifecycledDependency<RunningContainer>> dependencyCreator;

        public Builder(ContainerParametry containerParametry) {
            this.containerParametry = requireNonNull(containerParametry);
            dependencyCreator = new LocalDependencyCreator();
            preStartActions = new ArrayList<>();
        }

        public Builder addPreStartAction(ContainerAction action) {
            preStartActions.add(action);
            return this;
        }

        public Builder global() {
            this.dependencyCreator = new GlobalDependencyCreator();
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

        public ContainerDependencyRule build() {
            LifecycledDependency<RunningContainer> dependency = dependencyCreator.get();
            LazyContainerLifecycleRule rule = new LazyContainerLifecycleRule(dependency);
            return new ContainerDependencyRule(rule) {};
        }

    }

}

