package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.DefaultDockerManager;
import io.github.mike10004.containment.DockerManager;
import org.apache.maven.project.MavenProject;

public class MojoDockerManager extends DefaultDockerManager {

    public MojoDockerManager(DockerClientConfig clientConfig) {
        super(clientConfig);
    }

    public static DockerManager fromParametry(MavenProject project, RequireImageParametry parametry) {
        DefaultDockerClientConfig c = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .build();
        return new MojoDockerManager(c);

    }
}
