package io.github.mike10004.containment;

import java.util.List;

interface DockerPsContent {

    List<PortMapping> parsePortMappings();

    static DockerPsContent of(String jsonPsOutputForSingleContainer) {
        return new PsOutputParser(jsonPsOutputForSingleContainer);
    }

}
