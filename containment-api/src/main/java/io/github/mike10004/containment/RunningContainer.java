package io.github.mike10004.containment;

/**
 * Interface that represents a container that is currently running.
 */
public interface RunningContainer extends ActionableContainer {

    /**
     * Returns an inspector of this container.
     * @return an inspector
     */
    ContainerInspector inspector();

    /**
     * Returns an executor that can execute processes within the container.
     * @return an executor
     */
    ContainerExecutor executor();

    /**
     * Returns a service that provides access to container logs.
     * @return a log follower
     */
    ContainerLogFollower logs();

}
