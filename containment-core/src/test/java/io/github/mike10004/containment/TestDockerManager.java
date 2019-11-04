package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.easymock.EasyMock;

import java.util.function.Supplier;

public class TestDockerManager extends DefaultDjDockerManager {

    private TestDockerManager() {
        super(EasyMock.createMock(DockerClient.class), EasyMock.createMock(ContainerMonitor.class));
    }

    private static DockerClientConfig createClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    private static Supplier<DockerClient> configToSupplier(DockerClientConfig config) {
        return () -> DockerClientBuilder.getInstance(config).build();
    }

    private static DjDockerManager createManager(DockerClientConfig clientConfig) {
        ShutdownHookContainerMonitor monitor = new ShutdownHookContainerMonitor(configToSupplier(clientConfig));
        return new DefaultDjDockerManager(clientConfig, monitor);
    }

    private static final DjDockerManager INSTANCE = createManager(createClientConfig());

    /**
     * Returns a manager instance that uses a real client.
     * @return singleton instance
     */
    public static DjDockerManager getInstance() {
        return INSTANCE;
    }

    public static DjDockerManager createMock() {
        return new TestDockerManager();
    }
}
