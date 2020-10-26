package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DefaultDockerClientConfigBuildTest {

    @Test
    public void createConfig() {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        assertNotNull(clientConfig);
    }
}
