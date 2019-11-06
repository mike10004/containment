package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.ContainerCopier;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class DjStartableContainer implements StartableContainer {

    private final ContainerInfo info;
    private final DockerClient client;
    private final DjContainerMonitor containerMonitor;
    private final AtomicBoolean started;

    public DjStartableContainer(ContainerInfo info, DockerClient client, DjContainerMonitor containerMonitor) {
        this.info = requireNonNull(info, "info");
        this.client = requireNonNull(client);
        this.containerMonitor = requireNonNull(containerMonitor);
        started = new AtomicBoolean(false);
    }

    @Override
    public ContainerCopier copier() {
        return new DjContainerCopier(client, info);
    }

    @Override
    public ContainerInfo info() {
        return info;
    }

    @Override
    public void performAction(ContainerAction preStartAction) throws ContainmentException {
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

    @Override
    public synchronized StartedContainer start() throws ContainmentException {
        ContainerInfo info = info();
        try {
            client.startContainerCmd(info.id()).exec();
            started.getAndSet(true);
            containerMonitor.started(info.id());
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
        return new DjStartedContainer(client, info, containerMonitor);
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", DjStartableContainer.class.getSimpleName() + "[", "]")
                .add("info=" + info)
                .add("started=" + started)
                .toString();
    }
}
