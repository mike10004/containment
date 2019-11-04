package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.PortMapping;

import java.util.List;

public interface DockerPsContent {

    List<PortMapping> parsePortMappings();

    static DockerPsContent of(String jsonPsOutputForSingleContainer) {
        return new PsOutputParser(jsonPsOutputForSingleContainer);
    }

}
