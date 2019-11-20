package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.RunningContainer;

/**
 * Interface of an action that targets a started container.
 * Implementations of this interface do not require anything but the container instance
 * and do not produce anything.
 */
public interface ContainerPostStartRunnable {

    /**
     * Performs the action.
     * @param container container
     * @throws Exception on error
     */
    void perform(RunningContainer container) throws Exception;

}
