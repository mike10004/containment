package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DefaultDockerManager implements DockerManager {

    private final DockerClientConfig clientConfig;
    private final ContainerMonitor containerMonitor;

    public DefaultDockerManager(DockerClientConfig dockerClientConfig, ContainerMonitor containerMonitor) {
        this.clientConfig = requireNonNull(dockerClientConfig);
        this.containerMonitor = requireNonNull(containerMonitor);
    }

    protected static DockerClient buildClient(DockerClientConfig clientConfig) {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public final DockerClient openClient() {
        return DockerClientBuilder.getInstance(clientConfig).build();
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
