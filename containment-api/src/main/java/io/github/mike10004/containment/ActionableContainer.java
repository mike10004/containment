package io.github.mike10004.containment;

/**
 * Interface of a container that can accept certain actions.
 * The container may not be started yet.
 */
public interface ActionableContainer {

    /**
     * Returns a service that can be used to copy files to and from the container.
     * @return a copier service
     */
    ContainerCopier copier();

}
