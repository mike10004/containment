package io.github.mike10004.containment;

/**
 * Interface of a container that has been started.
 */
public interface StartedContainer extends RunningContainer, AutoCloseable {

    /**
     * Stops this container. If this container's auto-remove setting is not enabled,
     * then the container is manually removed.
     * @throws ContainmentException
     */
    @Override
    void close() throws ContainmentException;

}
