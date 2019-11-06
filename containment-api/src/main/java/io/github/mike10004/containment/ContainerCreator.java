package io.github.mike10004.containment;

import java.util.function.Consumer;

/**
 * Interface of a service that creates containers.
 */
public interface ContainerCreator extends AutoCloseable {

    /**
     * Creates a container and ignores warnings.
     * @param parametry container creation parameters
     * @return the container
     * @throws ContainmentException on error
     */
    default StartableContainer create(ContainerParametry parametry) throws ContainmentException {
        return create(parametry, ignore -> {});
    }

    /**
     * Creates a container.
     * @param parametry container creation parameters
     * @param warningListener consumer notified of warnings produced during the creation process
     * @return the container
     * @throws ContainmentException on error
     */
    StartableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException;

    /**
     * Removes the container.
     * @throws ContainmentException on error
     */
    @Override
    void close() throws ContainmentException;

}
