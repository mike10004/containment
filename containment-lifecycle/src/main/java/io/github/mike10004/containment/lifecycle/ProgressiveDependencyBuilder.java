package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class ProgressiveDependencyBuilder {

    private final Function<DockerClientConfig, DjContainerMonitor> shutdownHookMonitorFactory;
    private final Function<DockerClientConfig, DjContainerMonitor> manualMonitorFactory;

    private Consumer<? super LifecycleEvent> eventListener = LifecycleEvent.inactiveConsumer();

    protected ProgressiveDependencyBuilder() {
        this(ProgressiveDependencyBuilder::createShutdownHookContainerMonitor, ProgressiveDependencyBuilder::createManualContainerMonitor);
    }

    protected ProgressiveDependencyBuilder(Function<DockerClientConfig, DjContainerMonitor> shutdownHookMonitorFactory, Function<DockerClientConfig, DjContainerMonitor> manualMonitorFactory) {
        this.shutdownHookMonitorFactory = shutdownHookMonitorFactory;
        this.manualMonitorFactory = manualMonitorFactory;
    }

    public ProgressiveDependencyBuilder eventListener(Consumer<? super LifecycleEvent> eventListener) {
        this.eventListener = requireNonNull(eventListener);
        return this;
    }

    private static DjContainerMonitor createManualContainerMonitor(DockerClientConfig config) {
        return new DjManualContainerMonitor();
    }

    private static DjContainerMonitor createShutdownHookContainerMonitor(DockerClientConfig config) {
        return new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build());
    }

    /**
     * Builds a new local dependency instance. Local dependency instances
     * must have their {@link ContainerDependency#finishLifecycle()} invoked
     * explicitly to stop and remove the container, if it has been started.
     * @return a new instance
     */
    public <T> ProgressiveDependency<T> buildLocalDependency(ProgressiveLifecycleStack<T> stack) {
        return buildDependencyFromProvider(new LifecyclingCachingProvider<>(stack, eventListener));
    }

    /**
     * Builds a global dependency instance. The instance's {@link ContainerDependency#finishLifecycle()}
     * method will not actually cause the end of the container's lifecycle to be executed; that will
     * only happen upon JVM termination.
     * @return a new instance
     */
    public <T> ProgressiveDependency<T> buildGlobalDependency(ProgressiveLifecycleStack<T> stack) {
        return buildDependencyFromProvider(new GlobalLifecyclingCachingProvider<>(stack, eventListener));
    }

    private <T> ProgressiveDependency<T> buildDependencyFromProvider(LifecyclingCachingProvider<T> provider) {
        return ProgressiveDependency.fromProvider(provider);
    }

}
