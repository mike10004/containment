package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjDockerManager;

import java.util.function.Function;

/**
 * Constructor of creators for global containers. Cleanup of these containers is
 * managed by a {@link GlobalLifecyclingCachingProvider}, so they use
 * manual container monitors.
 */
class GlobalContainerCreatorFactory implements ContainerCreatorFactory {

    private final Function<? super DjDockerManager, ? extends ContainerCreator> djContainerCreatorFactory;
    private final Function<? super DockerClientConfig, DjContainerMonitor> manualContainerMonitorFactory;

    public GlobalContainerCreatorFactory(Function<? super DjDockerManager, ? extends ContainerCreator> djContainerCreatorFactory, Function<? super DockerClientConfig, DjContainerMonitor> manualContainerMonitorFactory) {
        this.djContainerCreatorFactory = djContainerCreatorFactory;
        this.manualContainerMonitorFactory = manualContainerMonitorFactory;
    }

    @Override
    public final ContainerCreator instantiate() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DjDockerManager manager = new DefaultDjDockerManager(config, manualContainerMonitorFactory.apply(config));
        return djContainerCreatorFactory.apply(manager);
    }
}
