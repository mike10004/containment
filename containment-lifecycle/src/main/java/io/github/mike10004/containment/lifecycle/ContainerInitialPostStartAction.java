package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.RunningContainer;

/**
 * Interface that provides a method to execute an action that targets a started container.
 * @param <P> result type
 */
public interface ContainerInitialPostStartAction<P> {

    /**
     * Executes the action.
     * @param container container
     * @return action result
     * @throws Exception on error
     */
    P perform(RunningContainer container) throws Exception;

}
