package io.github.mike10004.containment;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface of a container that has been started.
 */
public interface StartedContainer extends ActionableContainer, AutoCloseable {

    /**
     * Returns the container info.
     * @return container info
     */
    ContainerInfo info();

    /**
     * Stops this container. If this container's auto-remove setting is not enabled,
     * then the container is manually removed.
     * @throws ContainmentException
     */
    @Override
    void close() throws ContainmentException;

    /**
     * Fetch ports of this container that may be bound to host ports.
     * @return a list of possibly-bound container ports
     * @throws ContainmentException if port list could not be fetched
     */
    List<ContainerPort> fetchPorts() throws ContainmentException;

    /**
     * Starts following the standard output stream of the container process.
     * This method returns immediately, and the consumer accepts frames of output
     * asynchronously.
     * Frames of the output stream usually contain a single line of text output.
     * @param consumer the consumer
     * @param <C> consumer type
     * @return the argument consumer
     * @throws ContainmentException if container process standard output stream could not be followed
     */
    <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException;

    /**
     * Starts following the standard error stream of the container process.
     * This method returns immediately, and the consumer accepts frames of output
     * asynchronously.
     * Frames of the output stream usually contain a single line of text output.
     * @param consumer the consumer
     * @param <C> consumer type
     * @return the argument consumer
     * @throws ContainmentException if container process standard error stream could not be followed
     */
    <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException;

    /**
     * Returns an executor that can execute processes within the container.
     * @return an executor
     */
    ContainerExecutor executor();

}
