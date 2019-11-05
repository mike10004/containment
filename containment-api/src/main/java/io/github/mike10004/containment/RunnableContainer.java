package io.github.mike10004.containment;

/**
 * Interface of a runnable container. A runnable container has been created but
 * not yet started. To close a runnable container is to remove (destroy) it.
 */
public interface RunnableContainer extends AutoCloseable {

    /**
     * Gets the container info.
     * @return container info
     */
    ContainerInfo info();

    /**
     * Executes an action against the container.
     * @param preStartAction the action
     * @throws ContainmentException on error
     */
    void execute(PreStartAction preStartAction) throws ContainmentException;

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
