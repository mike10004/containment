package io.github.mike10004.containment.dockerjava;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.PortBinding;
import io.github.mike10004.containment.BindMount;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainmentException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class DjContainerCreator implements ContainerCreator {

    private final DockerClient client;
    private final DjContainerMonitor containerMonitor;

    public DjContainerCreator(DjDockerManager dockerManager) {
        this(dockerManager.openClient(), dockerManager.getContainerMonitor());
    }

    public DjContainerCreator(DockerClient client, DjContainerMonitor containerMonitor) {
        this.client = requireNonNull(client, "client");
        this.containerMonitor = requireNonNull(containerMonitor, "containerMonitor");
    }

    @Override
    public void close() throws ContainmentException {
        try {
            client.close();
        } catch (DockerException | IOException e) {
            throw new ContainmentException(e);
        }
    }

    protected HostConfig createHostConfig(ContainerParametry parametry) {
        List<PortBinding> bindings = parametry.bindablePorts().stream()
                .map(ContainerParametry.PortBinding::toSerialForm)
                .map(PortBinding::parse)
                .collect(Collectors.toList());
        Stream<Mount> bmounts = parametry.bindMounts().stream().map(DjContainerCreator::transformMount);
        Stream<Mount> tmounts = parametry.tmpfsMounts().stream().map(pathname -> {
            return new Mount().withTarget(pathname).withType(MountType.TMPFS);
        });
        return HostConfig.newHostConfig()
                .withAutoRemove(!parametry.disableAutoRemoveOnStop())
                .withMounts(Stream.concat(bmounts, tmounts).collect(Collectors.toList()))
                .withPortBindings(bindings);
    }

    private static Mount transformMount(BindMount apiMount) {
        return new Mount().withType(MountType.BIND)
                .withSource(apiMount.hostDirectory)
                .withTarget(apiMount.containerDirectory)
                .withReadOnly(apiMount.permission == BindMount.Permission.READ_ONLY);
    }

    /**
     * Creates the command object to be executed.
     * Override this method to customize the command that is created.
     * @param parametry parameter set
     * @return the command object
     */
    protected CreateContainerCmd constructCreateCommand(ContainerParametry parametry) {
        CreateContainerCmd createCmd = client.createContainerCmd(parametry.image().toString());
        HostConfig hostConfig = createHostConfig(parametry);
        createCmd.withHostConfig(hostConfig);
        List<String> command = parametry.command();
        if (!command.isEmpty()) {
            createCmd.withCmd(command);
        }
        List<String> envDefinitions = parametry.environment().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.toList());
        createCmd.withEnv(envDefinitions);
        createCmd.withLabels(parametry.labels());
        return createCmd;
    }

    protected File getTemporaryDirectory() {
        return FileUtils.getTempDirectory();
    }

    @Override
    public DjStartableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException {
        try {
            CreateContainerCmd createCmd = constructCreateCommand(parametry);
            CreateContainerResponse create = createCmd.exec();
            String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
            for (String warning : warnings) {
                warningListener.accept(warning);
            }
            String containerId = create.getId();
            containerMonitor.created(containerId);
            return new DjStartableContainer(ContainerInfo.define(containerId, parametry), client, containerMonitor);
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DjContainerCreator.class.getSimpleName() + "[", "]")
                .add("containerMonitor=" + containerMonitor)
                .toString();
    }
}
