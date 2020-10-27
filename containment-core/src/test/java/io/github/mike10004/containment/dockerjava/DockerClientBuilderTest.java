package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DockerClientBuilderTest {

    /**
     * This shows the correct way to build a client.
     * The old method that did not require an HTTP client implementation
     * is now deprecated.
     */
    @Test
    public void build() {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        assertNotNull(clientConfig);
        DockerClientBuilder.getInstance(clientConfig)
                .withDockerHttpClient(new JerseyDockerHttpClient.Builder()
                        .dockerHost(clientConfig.getDockerHost())
                        .sslConfig(clientConfig.getSSLConfig())
                .build());
    }
}
