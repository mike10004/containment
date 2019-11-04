package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerMonitor;
import io.github.mike10004.containment.DefaultDjDockerManager;

public class MojoDockerManager extends DefaultDjDockerManager {

    public MojoDockerManager(DockerClientConfig clientConfig, ContainerMonitor containerMonitor) {
        super(clientConfig, containerMonitor);
    }

}
