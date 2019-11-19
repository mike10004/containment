package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

/**
 * Interface that provides a method to execute an action against a running container.
 */
public interface ContainerPostStartRunnable {

    /**
     * Executes the action.
     * @param container container
     * @throws Exception on error
     */
    void perform(StartedContainer container) throws Exception;

}
