package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DefaultDockerManager implements DockerManager {

    private final DockerClient dockerClient;
    private final ContainerMonitor containerMonitor;

    public DefaultDockerManager(DockerClient dockerClient, ContainerMonitor containerMonitor) {
        this.dockerClient = requireNonNull(dockerClient);
        this.containerMonitor = requireNonNull(containerMonitor);
    }

    protected static DockerClient buildClient(DockerClientConfig clientConfig) {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public final DockerClient getClient() {
        return dockerClient;
    }

    @Override
    public List<Image> queryImagesByName(DockerClient client, String imageName) {
        return client.listImagesCmd().withImageNameFilter(imageName).exec();
    }

    @Override
    public ContainerMonitor getContainerMonitor() {
        return containerMonitor;
    }
}
