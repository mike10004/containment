package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

/**
 * Interface of a service that executes an action targeting a started container.
 */
public interface ContainerPostStartAction<R, P> {

    /**
     * Performs the action.
     * @param container the container
     * @throws Exception on error
     */
    P perform(StartedContainer container, R requirement) throws Exception;

}
