package io.github.mike10004.containment;

import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.ContainmentException;

public interface ActionableContainer {
    /**
     * Executes an action against the container.
     * @param action the action
     * @throws ContainmentException on error
     */
    void execute(ContainerAction action) throws ContainmentException;

}
