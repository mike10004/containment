package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;
import io.github.mike10004.containment.ContainmentException;

/**
 * Interface of an action that targets a container. The container
 * may not be started when the action is performed.
 */
public interface PreStartContainerStage<R, P> {

    /**
     * Performs the action. Note that the container may not be started.
     * @param container container info
     * @throws Exception on error
     */
    P perform(ActionableContainer container, R requirement) throws Exception;

}
