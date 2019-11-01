package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DefaultDockerManager implements DockerManager {

    private final DockerClientConfig clientConfig;
    private final ContainerMonitor containerMonitor;

    public DefaultDockerManager(DockerClientConfig clientConfig) {
        this(clientConfig, new ShutdownHookContainerMonitor(buildClient(clientConfig)));
    }

    public DefaultDockerManager(DockerClientConfig clientConfig, ContainerMonitor containerMonitor) {
        this.clientConfig = requireNonNull(clientConfig);
        this.containerMonitor = requireNonNull(containerMonitor);
    }

    private static DockerClient buildClient(DockerClientConfig clientConfig) {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public final DockerClient buildClient() {
        return buildClient(clientConfig);
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
