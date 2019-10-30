package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.concurrent.atomic.AtomicInteger;

public class TestDockerManager extends DefaultDockerManager {

    private final AtomicInteger buildClientCalls;

    public TestDockerManager(DockerClientConfig clientConfig) {
        super(clientConfig);
        buildClientCalls = new AtomicInteger(0);
    }

    @Override
    public DockerClient buildClient() {
        DockerClient client = super.buildClient();
        buildClientCalls.incrementAndGet();
        return client;
    }

    public int getNumBuildClientCalls() {
        return buildClientCalls.get();
    }

    public static DockerManager getInstance() {
        DefaultDockerClientConfig c = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();
        return new TestDockerManager(c);
    }
}
