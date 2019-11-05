package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;

public class MojoDockerManager extends DefaultDjDockerManager {

    public MojoDockerManager(DockerClientConfig clientConfig, DjContainerMonitor containerMonitor) {
        super(clientConfig, containerMonitor);
    }

}
