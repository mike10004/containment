/*
 * (c) 2013 [docker-java@googlegroups.com]
 * Apache License 2.0
 */

package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;

public class DockerClientBuilder {

    private DockerClientImpl dockerClient;

    private DockerCmdExecFactory dockerCmdExecFactory = null;

    private DockerClientBuilder(DockerClientImpl dockerClient) {
        this.dockerClient = dockerClient;
    }

    public static DockerClientBuilder getInstance() {
        return new DockerClientBuilder(DockerClientImpl.getInstance());
    }

    public static DockerClientBuilder getInstance(Builder dockerClientConfigBuilder) {
        return getInstance(dockerClientConfigBuilder.build());
    }

    public static DockerClientBuilder getInstance(DockerClientConfig dockerClientConfig) {
        return new DockerClientBuilder(DockerClientImpl.getInstance(dockerClientConfig));
    }

    public static DockerClientBuilder getInstance(String serverUrl) {
        return new DockerClientBuilder(DockerClientImpl.getInstance(serverUrl));
    }

    public static DockerCmdExecFactory getDefaultDockerCmdExecFactory() {
        try {
            return (DockerCmdExecFactory) Class.forName("com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory")
                        .getConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new RuntimeException("BUG: classpath is misconfigured or an update to docker-java has invalidated this method of construction", e);
        }
    }

    public DockerClientBuilder withDockerCmdExecFactory(DockerCmdExecFactory dockerCmdExecFactory) {
        this.dockerCmdExecFactory = dockerCmdExecFactory;
        return this;
    }

    public DockerClient build() {
        if (dockerCmdExecFactory != null) {
            dockerClient.withDockerCmdExecFactory(dockerCmdExecFactory);
        } else {
            dockerClient.withDockerCmdExecFactory(getDefaultDockerCmdExecFactory());
        }

        return dockerClient;
    }
}
