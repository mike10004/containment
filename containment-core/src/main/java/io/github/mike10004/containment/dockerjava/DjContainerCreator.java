package io.github.mike10004.containment.dockerjava;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.PreStartAction;
import io.github.mike10004.containment.RunnableContainer;
import io.github.mike10004.containment.RunningContainer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private class CopyFileAction implements PreStartAction {

        private final File srcFile;
        private final String destinationPathname;

        private CopyFileAction(File srcFile, String destinationPathname) {
            this.srcFile = srcFile;
            this.destinationPathname = destinationPathname;
        }

        @Override
        public void perform(ContainerInfo unstartedContainer) throws ContainmentException {
            copyFileToContainer(unstartedContainer.id(), srcFile, destinationPathname);
        }
    }

    private void copyFileToContainer(String containerId, File srcFile, String destinationPathname) throws ContainmentException {
        try {
            client.copyArchiveToContainerCmd(containerId)
                    .withHostResource(srcFile.getAbsolutePath())
                    .withRemotePath(destinationPathname)
                    .exec();
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

    protected File getTemporaryDirectory() {
        return FileUtils.getTempDirectory();
    }

    private class CopyBytesAction implements PreStartAction {

        private final File tmpDir;
        private final byte[] sourceBytes;
        private final String destinationPathname;

        public CopyBytesAction(File tmpDir, byte[] sourceBytes, String destinationPathname) {
            this.tmpDir = tmpDir;
            this.sourceBytes = sourceBytes;
            this.destinationPathname = destinationPathname;
        }

        @Override
        public void perform(ContainerInfo unstartedContainer) throws ContainmentException {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("pre-start-action", ".tmp", tmpDir);
                java.nio.file.Files.write(tempFile.toPath(), sourceBytes, StandardOpenOption.WRITE);
                copyFileToContainer(unstartedContainer.id(), tempFile, destinationPathname);
            } catch (DockerException | IOException e) {
                throw new ContainmentException(e);
            } finally {
                if (tempFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            }
        }
    }

    public class DjRunnableContainer implements RunnableContainer {

        private final ContainerInfo info;
        private final AtomicBoolean started;
        private final boolean autoRemove;

        private DjRunnableContainer(ContainerInfo info, boolean autoRemove) {
            this.info = requireNonNull(info, "info");
            started = new AtomicBoolean(false);
            this.autoRemove = autoRemove;
        }

        @Override
        public ContainerInfo info() {
            return info;
        }

        @Override
        public void execute(PreStartAction preStartAction) throws ContainmentException {
            preStartAction.perform(info);
        }

        @Override
        public synchronized void close() throws ContainmentException {
            maybeRemove();
            containerMonitor.removed(info.id());
        }

        private void maybeRemove() throws ContainmentException {
            boolean hasBeenStarted = started.get();
            if (hasBeenStarted && autoRemove) {
                /*
                 * Then the container will be removed when it stops, so we don't
                 * have to do remove it explicitly.
                 */
                return;
            }
            String containerId = info.id();
            try {
                client.removeContainerCmd(containerId).withForce(true).exec();
            } catch (DockerException e) {
                throw new ContainmentException(e);
            }
        }

        /**
         * Adds a pre-start action that copies a file to the container.
         * @param sourceFile
         * @param destinationPathname
         */
        public void copyToContainer(File sourceFile, String destinationPathname) throws ContainmentException {
            execute(new CopyFileAction(sourceFile, destinationPathname));
        }

        public void copyToContainer(byte[] sourceBytes, String destinationPathname) throws ContainmentException {
            execute(new CopyBytesAction(getTemporaryDirectory(), sourceBytes, destinationPathname));
        }

        @Override
        public synchronized RunningContainer start() throws ContainmentException {
            ContainerInfo info = info();
            try {
                client.startContainerCmd(info.id()).exec();
                started.getAndSet(true);
                containerMonitor.started(info.id());
            } catch (DockerException e) {
                throw new ContainmentException(e);
            }
            return new DjRunningContainer(client, info, containerMonitor);
        }

    }

    @Override
    public DjRunnableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException {
        try {
            CreateContainerCmd createCmd = applyParametry(parametry);
            CreateContainerResponse create = createCmd.exec();
            String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
            for (String warning : warnings) {
                warningListener.accept(warning);
            }
            String containerId = create.getId();
            containerMonitor.created(containerId);
            boolean autoRemoveEnabled = !parametry.disableAutoRemove();
            return new DjRunnableContainer(ContainerInfo.define(containerId), autoRemoveEnabled);
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

}
