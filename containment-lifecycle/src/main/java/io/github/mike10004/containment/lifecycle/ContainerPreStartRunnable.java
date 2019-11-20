package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

/**
 * Interface of an action that targets an unstarted container.
 * Implementations of this interface do not require anything but the container instance
 * and do not produce anything.
 */
public interface ContainerPreStartRunnable {

    /**
     * Performs the action.
     * @param container container
     * @throws Exception on error
     */
    void perform(ActionableContainer container) throws Exception;

}
