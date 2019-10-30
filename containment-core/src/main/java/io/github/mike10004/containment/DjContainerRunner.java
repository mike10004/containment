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
import java.util.ArrayList;
import java.util.Arrays;
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
        CreateContainerCmd createCmd = client.createContainerCmd(parametry.image().toString());
        List<PortBinding> bindings = parametry.exposedPorts().stream().map(portNumber -> {
            return new PortBinding(Ports.Binding.empty(), new ExposedPort(portNumber));
        }).collect(Collectors.toList());
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(!parametry.disableAutoRemove())
                .withPortBindings(bindings);
        createCmd.withHostConfig(hostConfig);
        createCmd.withCmd(parametry.command());
        List<String> envDefinitions = parametry.environment().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.toList());
        createCmd.withEnv(envDefinitions);
        return createCmd;
    }

    private class DjRunnableContainer implements RunnableContainer {

        private final List<PreStartAction> preStartActions;

        private String containerId;
        private String[] warnings;

        public DjRunnableContainer(String containerId, String[] warnings) {
            this.containerId = containerId;
            this.warnings = warnings;
            preStartActions = new ArrayList<>();
        }

        @Override
        public CreatedContainer info() {
            return new CreatedContainer() {
                @Override
                public String getId() {
                    return containerId;
                }

                @Override
                public List<String> getWarnings() {
                    return Arrays.asList(warnings);
                }
            };
        }

        @Override
        public synchronized RunnableContainer prepare(PreStartAction preStartAction) {
            preStartActions.add(preStartAction);
            return this;
        }

        private void tryRemoval() {
            client.removeContainerCmd(containerId).withForce(true);
        }

        @Override
        public synchronized RunningContainer start() throws ContainmentException {
            CreatedContainer info = info();
            try {
                for (PreStartAction action : preStartActions) {
                    action.perform(info);
                }
            } catch (Exception e) {
                ContainmentException percolator = null;
                try {
                    tryRemoval();
                } catch (Exception removalError) {
                    percolator = new ContainmentException("pre-start action failed and removal of unstarted container failed due to " + removalError, e);
                }
                if (percolator == null) {
                    throw new ContainmentException("pre-start action failed", e);
                }
                throw percolator;
            }
            try {
                client.startContainerCmd(containerId).exec();
            } catch (DockerException e) {
                throw new ContainmentException(e);
            }
            return new DjRunningContainer(client, containerId);
        }

    }

    @Override
    public RunnableContainer create(ContainerParametry parametry) {
        CreateContainerCmd createCmd = applyParametry(parametry);
        CreateContainerResponse create = createCmd.exec();
        String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
        String containerId = create.getId();
        return new DjRunnableContainer(containerId, warnings);
    }

}
