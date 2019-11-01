package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.DefaultDockerManager;
import io.github.mike10004.containment.DockerClientBuilder;
import io.github.mike10004.containment.DockerManager;
import org.apache.maven.project.MavenProject;

public class MojoDockerManager extends DefaultDockerManager {

    public MojoDockerManager(DockerClient client) {
        super(client);
    }

    public static DockerManager fromParametry(MavenProject project, RequireImageParametry parametry) {
        DefaultDockerClientConfig c = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .build();
        DockerClient client = DockerClientBuilder.getInstance(c).build();
        return new MojoDockerManager(client);

    }
}
