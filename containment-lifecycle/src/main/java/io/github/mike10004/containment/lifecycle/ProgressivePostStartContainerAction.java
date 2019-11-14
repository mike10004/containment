package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

/**
 * Interface of a service that executes an action targeting a started container.
 */
public interface ProgressivePostStartContainerAction<R, P> {

    /**
     * Performs the action.
     * @param container the container
     * @throws Exception on error
     */
    P perform(StartedContainer container, R requirement) throws Exception;

    interface IndependentPostStartAction<P> extends ProgressivePostStartContainerAction<Void, P> {

        @Override
        default P perform(StartedContainer container, Void requirement) throws Exception {
            return perform(container);
        }

        P perform(StartedContainer container) throws Exception;
    }
}
