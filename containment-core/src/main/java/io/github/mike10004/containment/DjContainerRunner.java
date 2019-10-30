package io.github.mike10004.containment;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DjContainerRunner implements ContainerRunner {

    private final DockerClient client;

    public DjContainerRunner() {
        this(buildDockerClient());
    }

    public DjContainerRunner(DockerClient client) {
        this.client = requireNonNull(client);
    }

    @Override
    public void close() throws ContainmentException {
        try {
            client.close();
        } catch (DockerException | IOException e) {
            throw new ContainmentException(e);
        }
    }

    protected CreateContainerCmd applyParametry(ContainerParametry parametry) {
        CreateContainerCmd createCmd = client.createContainerCmd(parametry.image);
        List<PortBinding> bindings = parametry.exposedPorts.stream().map(portNumber -> {
            return new PortBinding(Ports.Binding.empty(), new ExposedPort(portNumber));
        }).collect(Collectors.toList());
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(true)
                .withPortBindings(bindings);
        createCmd.withHostConfig(hostConfig);
        createCmd.withCmd(parametry.command);
        createCmd.withEnv(parametry.env.entrySet().stream().map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.toList()));
        createCmd.withAttachStdout(true);
        return createCmd;
    }

    @Override
    public RunningContainer run(ContainerParametry parametry) {
        CreateContainerCmd createCmd = applyParametry(parametry);
        CreateContainerResponse create = createCmd.exec();
        String containerId = create.getId();
        String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
        for (String warning : warnings) {
            System.err.println(warning);
        }
        client.startContainerCmd(containerId)
                .exec();
        return new DjRunningContainer(client, containerId);
    }

    private static DockerClient buildDockerClient() {
        return DockerClientBuilder.getInstance(buildClientConfig()).build();
    }

    private static DockerClientConfig buildClientConfig() {
        DefaultDockerClientConfig c = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();
        return c;
    }

}
