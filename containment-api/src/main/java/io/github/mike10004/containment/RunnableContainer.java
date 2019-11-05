package io.github.mike10004.containment;

/**
 * Interface of a runnable container. A runnable container has been created but
 * not yet started. To close a runnable container is to remove (destroy) it.
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
     * Removes/destroys this container.
     * @throws ContainmentException
     */
    void close() throws ContainmentException;
}
