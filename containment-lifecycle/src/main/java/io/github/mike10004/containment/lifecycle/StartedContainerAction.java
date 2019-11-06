package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartedContainer;

/**
 * Interface of a service that executes an action targeting a started container.
 */
public interface StartedContainerAction {

    /**
     * Performs the action.
     * @param container the container
     * @throws ContainmentException on error
     */
    void perform(StartedContainer container) throws ContainmentException;
}
