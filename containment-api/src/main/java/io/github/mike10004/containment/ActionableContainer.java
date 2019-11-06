package io.github.mike10004.containment;

/**
 * Interface of a container that supports actions targeted at itself.
 */
public interface ActionableContainer {

    /**
     * Executes an action against targeted at the container.
     * @param action the action
     * @throws ContainmentException on error
     */
    void execute(ContainerAction action) throws ContainmentException;

    /**
     * Returns a service that can be used to copy files to and from the container.
     * @return a copier service
     */
    ContainerCopier copier();

}
