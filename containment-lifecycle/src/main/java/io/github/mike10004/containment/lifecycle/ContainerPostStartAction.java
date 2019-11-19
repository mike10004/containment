package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.RunningContainer;

/**
 * Interface that provides a method to execute an action targeting a started container.
 */
public interface ContainerPostStartAction<R, P> {

    /**
     * Performs the action.
     * @param container the container
     * @throws Exception on error
     */
    P perform(RunningContainer container, R requirement) throws Exception;

}
