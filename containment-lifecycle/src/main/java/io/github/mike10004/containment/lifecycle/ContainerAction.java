package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainmentException;

/**
 * Interface of an action that targets a container. The container
 * may not be started when the action is performed.
 */
public interface ContainerAction {

    /**
     * Performs the action. Note that the container may not be started.
     * @param container container info
     * @throws ContainmentException
     */
    void perform(ContainerInfo container) throws ContainmentException;

}
