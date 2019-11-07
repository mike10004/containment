package io.github.mike10004.containment;

/**
 * Interface of a startable container that is not yet started but can be started.
 * To close a runnable container is to remove (destroy) it.
 */
public interface StartableContainer extends ActionableContainer, AutoCloseable {

    /**
     * Starts the container. Creates a new instance of {@link StartedContainer}.
     * @return a running container instance
     * @throws ContainmentException on error
     */
    StartedContainer start() throws ContainmentException;

    /**
     * Removes this container. If the container's auto-remove setting is enabled
     * and the container was started, then this does nothing, because it will be
     * automatically removed. Otherwise, it is explicitly removed.     *
     * @throws ContainmentException
     */
    void close() throws ContainmentException;
}
