/*
 * (c) 2013 [docker-java@googlegroups.com]
 * Apache License 2.0
 */

package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerClientBuilder {

    private final DockerClientConfig dockerClientConfig;

    private DockerHttpClient dockerHttpClient = null;

    private DockerClientBuilder(DockerClientConfig dockerClientConfig) {
        this.dockerClientConfig = dockerClientConfig;
    }

    public static DockerClientBuilder getInstance() {
        return new DockerClientBuilder(
                DefaultDockerClientConfig.createDefaultConfigBuilder().build()
        );
    }

    public static DockerClientBuilder getInstance(DockerClientConfig dockerClientConfig) {
        return new DockerClientBuilder(dockerClientConfig);
    }

    /**
     * Note that this method overrides {@link DockerCmdExecFactory} if it was previously set
     */
    public DockerClientBuilder withDockerHttpClient(DockerHttpClient dockerHttpClient) {
        this.dockerHttpClient = dockerHttpClient;
        return this;
    }

    public DockerClient build() {
        if (dockerHttpClient != null) {
            return DockerClientImpl.getInstance(
                    dockerClientConfig,
                    dockerHttpClient
            );
        } else {
            Logger log = LoggerFactory.getLogger(DockerClientBuilder.class);
            log.warn(
                    "'dockerHttpClient' should be set." +
                            "Falling back to Jersey, will be an error in future releases."
            );

            return DockerClientImpl.getInstance(
                    dockerClientConfig,
                    new JerseyDockerHttpClient.Builder()
                            .dockerHost(dockerClientConfig.getDockerHost())
                            .sslConfig(dockerClientConfig.getSSLConfig())
                            .build()
            );
        }
    }
}
