package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.ContainerPort;

import java.util.List;

public interface DockerPsContent {

    List<ContainerPort> parsePortMappings();

    static DockerPsContent of(String jsonPsOutputForSingleContainer) {
        return new PsOutputParser(jsonPsOutputForSingleContainer);
    }

}
