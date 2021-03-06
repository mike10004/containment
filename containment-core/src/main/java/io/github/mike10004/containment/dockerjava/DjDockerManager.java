package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;

import java.io.IOException;
import java.util.List;

public interface DjDockerManager {

    DockerClient openClient();

    default boolean queryImageExistsLocally(DockerClient client, String imageName) {
        return !queryImagesByName(client, imageName).isEmpty();
    }

    default boolean queryImageExistsLocally(String imageName) throws IOException {
        try (DockerClient client = openClient()) {
            return queryImageExistsLocally(client, imageName);
        }
    }

    List<Image> queryImagesByName(DockerClient client, String imageName);

    DjContainerMonitor getContainerMonitor();
}
