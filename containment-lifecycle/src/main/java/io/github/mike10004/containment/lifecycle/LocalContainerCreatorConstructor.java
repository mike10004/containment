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
 * Constructor of creators for local containers. Cleanup of these containers is managed
 * by the caller, but a global monitor adds a shutdown hook to clean up ones the caller forgot.
 */
class LocalContainerCreatorConstructor implements ContainerCreatorConstructor {

    private final Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor;
    private final Function<? super DockerClientConfig, DjContainerMonitor> shutdownHookMonitorFactory;

    public LocalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor, Function<? super DockerClientConfig, DjContainerMonitor> shutdownHookMonitorFactory) {
        this.djConstructor = djConstructor;
        this.shutdownHookMonitorFactory = shutdownHookMonitorFactory;
    }

    @Override
    public final ContainerCreator instantiate() throws ContainmentException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DjDockerManager manager = new DefaultDjDockerManager(config, shutdownHookMonitorFactory.apply(config));
        return djConstructor.apply(manager);
    }
}
