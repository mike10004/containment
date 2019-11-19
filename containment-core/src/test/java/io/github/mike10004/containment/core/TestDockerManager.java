package io.github.mike10004.containment.core;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import org.easymock.EasyMock;

import java.util.function.Supplier;

public class TestDockerManager extends DefaultDjDockerManager {

    private TestDockerManager() {
        super(EasyMock.createMock(DockerClient.class), EasyMock.createMock(DjContainerMonitor.class));
    }

    static DockerClientConfig createClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    private static Supplier<DockerClient> configToSupplier(DockerClientConfig config) {
        return () -> DockerClientBuilder.getInstance(config).build();
    }

    private static DjDockerManager createManager(DockerClientConfig clientConfig) {
        DjShutdownHookContainerMonitor monitor = new DjShutdownHookContainerMonitor(configToSupplier(clientConfig));
        return new DefaultDjDockerManager(clientConfig, monitor);
    }

    private static final DjDockerManager INSTANCE = createManager(createClientConfig());

    /**
     * Returns a manager instance that uses a real client and a global shutdown hook
     * to stop and remove any containers.
     * @return singleton instance
     */
    public static DjDockerManager getInstance() {
        return INSTANCE;
    }

    public static DjDockerManager createMock() {
        return new TestDockerManager();
    }
}
