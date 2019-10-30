package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class DefaultDockerManager implements DockerManager {

    private final DockerClientConfig clientConfig;

    public DefaultDockerManager(DockerClientConfig clientConfig) {
        this.clientConfig = requireNonNull(clientConfig);
    }

    @Override
    public DockerClient buildClient() {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public List<Image> queryImagesByName(DockerClient client, String imageName) {
        return client.listImagesCmd().withImageNameFilter(imageName).exec();
    }

}
