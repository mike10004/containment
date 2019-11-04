package io.github.mike10004.containment.dockerjava;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Interface of a service that monitors the lifecycle of containers.
 */
public interface ContainerMonitor {

    /**
     * Notifies the monitor that a container was created.
     * If {@link #removed(String)}
     * @param containerId container id
     */
    void created(String containerId);

    /**
     * Notifies the monitor that a container was started.
     * @param containerId container id
     */
    void started(String containerId);

    /**
     * Notifies the monitor that a container was stopped.
     * @param containerId container id
     */
    void stopped(String containerId);

    /**
     * Notifies the monitor that a container was removed.
     * @param containerId container id
     */
    void removed(String containerId);

}

