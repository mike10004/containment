package io.github.mike10004.containment;

/**
 * Interface of a value that represents information about a container.
 */
public interface ContainerInfo {

    /**
     * Creates a new instance.
     * @param containerId container id
     * @param parametry container creation parameters
     * @return a new instance
     */
    static ContainerInfo define(String containerId, ContainerParametry parametry) {
        Stickiness stickiness = parametry.disableAutoRemoveOnStop() ? Stickiness.MANUAL_REMOVE_REQUIRED : Stickiness.AUTO_REMOVE_ENABLED;
        return define(containerId, stickiness, parametry.commandType());
    }

    /**
     * Creates a new instance.
     * @param containerId container id
     * @param stickiness container stickiness
     * @param commandType container
     * @return
     */
    static ContainerInfo define(String containerId, Stickiness stickiness, ContainerParametry.CommandType commandType) {
        return new ContainerInfoImpl(containerId, stickiness, commandType);
    }

    /**
     * Returns the container id.
     * @return container id
     */
    String id();

    /**
     * Checks whether auto-remove is enabled. Auto-remove causes a container to be removed when it is stopped.
     * @return true if auto-remove is enabled
     * @see Stickiness
     */
    boolean isAutoRemoveEnabled();

    /**
     * Checks whether the container must be stopped explicitly. Containers started with commands that exit
     * without being actively terminated do not need to be stopped explicitly. A container that
     * remains alive indefinitely after being started must be stopped explicitly.
     * @return true if explicit stop is required
     * @see io.github.mike10004.containment.ContainerParametry.CommandType
     */
    boolean isStopRequired();

    /**
     * Enumeration of constants that represent a container's auto-removal property.
     * Auto-remove means that a container is removed automatically once it is stopped.
     */
    enum Stickiness {
        /**
         * Auto remove is enabled.
         */
        AUTO_REMOVE_ENABLED,

        /**
         * Manual removal is required because auto-remove is not enabled.
         */
        MANUAL_REMOVE_REQUIRED
    }

}
