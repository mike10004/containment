package io.github.mike10004.containment;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface of a container that has been started.
 */
public interface RunningContainer extends ActionableContainer, AutoCloseable {

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

    List<PortMapping> fetchPorts() throws ContainmentException;

    <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException;

    <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException;

}
