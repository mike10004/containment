package io.github.mike10004.containment;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DjContainerRunner implements ContainerRunner {

    private final DockerClient client;

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
        List<String> envDefinitions = parametry.env.entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.toList());
        createCmd.withEnv(envDefinitions);
        return createCmd;
    }

    @Override
    public RunningContainer run(ContainerParametry parametry, CreationObserver creationObserver) {
        CreateContainerCmd createCmd = applyParametry(parametry);
        CreateContainerResponse create = createCmd.exec();
        String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
        for (String warning : warnings) {
            creationObserver.warn(warning);
        }
        String containerId = create.getId();
        creationObserver.prepare(containerId);
        client.startContainerCmd(containerId)
                .exec();
        return new DjRunningContainer(client, containerId);
    }

}
