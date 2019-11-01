package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import org.easymock.EasyMock;

public class TestDockerManager extends DefaultDockerManager {

    private TestDockerManager() {
        super(EasyMock.createMock(DockerClient.class), EasyMock.createMock(ContainerMonitor.class));
    }

    private static DockerClient createClient() {
        return DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();
    }

    private static DockerManager createManager(DockerClient client) {
        ShutdownHookContainerMonitor monitor = new ShutdownHookContainerMonitor(client);
        return new DefaultDockerManager(client, monitor);
    }

    private static final DockerManager INSTANCE = createManager(createClient());

    /**
     * Returns a manager instance that uses a real client.
     * @return singleton instance
     */
    public static DockerManager getInstance() {
        return INSTANCE;
    }

    public static DockerManager createMock() {
        return new TestDockerManager();
    }
}
