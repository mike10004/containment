package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

/**
 * Interface of an action that targets a container. The container
 * is not yet started when the action is performed.
 * When used to build a container lifecycle, this becomes a lifecycle stage.
 * @param <R> type of resource required
 * @param <P> type of resource produced
 */
public interface ContainerPreStartAction<R, P> {

    /**
     * Performs the action. Note that the container may not be started.
     * @param container container info
     * @param requirement required resource
     * @return produced resource
     * @throws Exception on error
     */
    P perform(ActionableContainer container, R requirement) throws Exception;

}
