package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjDockerManager;

import java.util.function.Function;

/**
 * Constructor of creators for global containers. Cleanup of these containers is
 * managed by a {@link GlobalLifecyclingCachingProvider}, so they use
 * manual container monitors.
 */
class GlobalContainerCreatorConstructor implements ContainerCreatorConstructor {

    private final Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor;
    private final Function<? super DockerClientConfig, DjContainerMonitor> manualContainerMonitorFactory;

    public GlobalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor, Function<? super DockerClientConfig, DjContainerMonitor> manualContainerMonitorFactory) {
        this.djConstructor = djConstructor;
        this.manualContainerMonitorFactory = manualContainerMonitorFactory;
    }

    @Override
    public final ContainerCreator instantiate() throws ContainmentException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DjDockerManager manager = new DefaultDjDockerManager(config, manualContainerMonitorFactory.apply(config));
        return djConstructor.apply(manager);
    }
}
