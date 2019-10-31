package io.github.mike10004.containment;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
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

    private class CopyFileAction implements PreStartAction {

        private final File srcFile;
        private final String destinationPathname;

        private CopyFileAction(File srcFile, String destinationPathname) {
            this.srcFile = srcFile;
            this.destinationPathname = destinationPathname;
        }

        @Override
        public void perform(CreatedContainer unstartedContainer) {
            copyFileToContainer(unstartedContainer.getId(), srcFile, destinationPathname);
        }
    }

    private void copyFileToContainer(String containerId, File srcFile, String destinationPathname) {
        client.copyArchiveToContainerCmd(containerId)
                .withHostResource(srcFile.getAbsolutePath())
                .withRemotePath(destinationPathname)
                .exec();

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
        public void perform(CreatedContainer unstartedContainer) throws ContainmentException {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("pre-start-action", ".tmp", tmpDir);
                java.nio.file.Files.write(tempFile.toPath(), sourceBytes, StandardOpenOption.WRITE);
                copyFileToContainer(unstartedContainer.getId(), tempFile, destinationPathname);
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

        private final List<PreStartAction> preStartActions;

        private String containerId;
        private String[] warnings;

        private DjRunnableContainer(String containerId, String[] warnings) {
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
        public synchronized DjRunnableContainer prepare(PreStartAction preStartAction) {
            preStartActions.add(preStartAction);
            return this;
        }

        private void tryRemoval() {
            client.removeContainerCmd(containerId).withForce(true);
        }

        /**
         * Adds a pre-start action that copies a file to the container.
         * @param sourceFile
         * @param destinationPathname
         * @return
         */
        public DjRunnableContainer copyToContainer(File sourceFile, String destinationPathname) {
            return prepare(new CopyFileAction(sourceFile, destinationPathname));
        }

        public DjRunnableContainer copyToContainer(byte[] sourceBytes, String destinationPathname) {
            return prepare(new CopyBytesAction(getTemporaryDirectory(), sourceBytes, destinationPathname));
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
                    if (e instanceof ContainmentException) {
                        throw (ContainmentException) e;
                    } else {
                        throw new ContainmentException("pre-start action failed", e);
                    }
                } else {
                    throw percolator;
                }
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
    public DjRunnableContainer create(ContainerParametry parametry) throws ContainmentException {
        try {
            CreateContainerCmd createCmd = applyParametry(parametry);
            CreateContainerResponse create = createCmd.exec();
            String[] warnings = ArrayUtil.nullToEmpty(create.getWarnings());
            String containerId = create.getId();
            return new DjRunnableContainer(containerId, warnings);
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

}
