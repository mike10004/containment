package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.transport.DockerHttpClient;

import java.util.List;

import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;

import static java.util.Objects.requireNonNull;

public class DefaultDjDockerManager implements DjDockerManager {

    private final DockerClientConfig clientConfig;
    private final DjContainerMonitor containerMonitor;

    public DefaultDjDockerManager(DockerClientConfig dockerClientConfig, DjContainerMonitor containerMonitor) {
        this.clientConfig = requireNonNull(dockerClientConfig);
        this.containerMonitor = requireNonNull(containerMonitor);
    }

    protected static DockerClient buildClient(DockerClientConfig clientConfig) {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public final DockerClient openClient() {
        DockerHttpClient httpClient = new JerseyDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .build();
        return DockerClientBuilder.getInstance(clientConfig)
                .withDockerHttpClient(httpClient)
                .build();
    }

    @Override
    public List<Image> queryImagesByName(DockerClient client, String imageName) {
        return client.listImagesCmd().withImageNameFilter(imageName).exec();
    }

    @Override
    public DjContainerMonitor getContainerMonitor() {
        return containerMonitor;
    }
}
