package io.github.mike10004.containment;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

public class TestDockerManager extends DefaultDockerManager {

    private TestDockerManager(DockerClientConfig clientConfig) {
        super(clientConfig);
    }

    private static final DockerManager INSTANCE = new TestDockerManager(DefaultDockerClientConfig.createDefaultConfigBuilder().build());

    public static DockerManager getInstance() {
        return INSTANCE;
    }
}
