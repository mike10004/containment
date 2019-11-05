package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.PreStartAction;
import io.github.mike10004.containment.RunnableContainer;
import io.github.mike10004.containment.RunningContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class DjRunnableContainer implements RunnableContainer {

    private final ContainerInfo info;
    private final DockerClient client;
    private final DjContainerMonitor containerMonitor;
    private final AtomicBoolean started;

    public DjRunnableContainer(ContainerInfo info, DockerClient client, DjContainerMonitor containerMonitor) {
        this.info = requireNonNull(info, "info");
        this.client = requireNonNull(client);
        this.containerMonitor = requireNonNull(containerMonitor);
        started = new AtomicBoolean(false);
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
        if (hasBeenStarted && info.isAutoRemoveEnabled()) {
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

    public void copyToContainer(byte[] sourceBytes, String destinationPathname, File localTempDirectory) throws ContainmentException {
        execute(new CopyBytesAction(localTempDirectory, sourceBytes, destinationPathname));
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

}
