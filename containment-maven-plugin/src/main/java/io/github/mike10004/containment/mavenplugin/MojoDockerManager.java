package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerMonitor;
import io.github.mike10004.containment.DefaultDockerManager;
import org.apache.maven.project.MavenProject;

public class MojoDockerManager extends DefaultDockerManager {

    public MojoDockerManager(DockerClientConfig clientConfig, ContainerMonitor containerMonitor) {
        super(clientConfig, containerMonitor);
    }

}
