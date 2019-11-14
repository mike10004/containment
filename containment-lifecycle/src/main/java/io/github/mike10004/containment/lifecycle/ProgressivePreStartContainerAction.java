package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

/**
 * Interface of an action that targets a container. The container
 * may not be started when the action is performed.
 */
public interface ProgressivePreStartContainerAction<R, P> {

    /**
     * Performs the action. Note that the container may not be started.
     * @param container container info
     * @throws Exception on error
     */
    P perform(ActionableContainer container, R requirement) throws Exception;

    interface IndependentPreStartAction<P> extends ProgressivePreStartContainerAction<Void, P> {

        @Override
        default P perform(ActionableContainer container, Void requirement) throws Exception {
            return perform(container);
        }

        P perform(ActionableContainer container) throws Exception;
    }
}
