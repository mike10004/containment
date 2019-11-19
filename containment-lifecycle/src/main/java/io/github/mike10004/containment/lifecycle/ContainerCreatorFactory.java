package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainmentException;

/**
 * Interface of a service that constructs a container creator.
 */
public interface ContainerCreatorFactory {

    /**
     * Instantiates the container creator.
     * @return a creator instance
     * @throws ContainmentException on error
     */
    ContainerCreator instantiate() throws ContainmentException;

}
