package io.github.mike10004.containment;

/**
 * Interface of a startable container that is not yet started but can be started.
 * To close a runnable container is to remove (destroy) it.
 */
public interface RunnableContainer extends ActionableContainer, AutoCloseable {

    /**
     * Gets the container info.
     * @return container info
     */
    ContainerInfo info();

    /**
     * Starts the container. Creates a new instance of {@link RunningContainer}.
     * @return a running container instance
     * @throws ContainmentException on error
     */
    RunningContainer start() throws ContainmentException;

    /**
     * Removes this container. If the container's auto-remove setting is enabled
     * and the container was started, then this does nothing, because it will be
     * automatically removed. Otherwise, it is explicitly removed.     *
     * @throws ContainmentException
     */
    void close() throws ContainmentException;
}
