package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.RunningContainer;

/**
 * Interface that provides a method to execute an action targeting a started container.
 * When used to build a container lifecycle, this becomes a lifecycle stage.
 * @param <R> type of resource required
 * @param <P> type of resource produced
 */
public interface ContainerPostStartAction<R, P> {

    /**
     * Performs the action.
     * @param container container
     * @return produced resource
     * @throws Exception on error
     */
    P perform(RunningContainer container, R requirement) throws Exception;

}
