package io.github.mike10004.containment;

public interface ContainerInfo {

    static ContainerInfo define(String containerId, ContainerParametry parametry) {
        Stickiness stickiness = parametry.disableAutoRemove() ? Stickiness.MANUAL_REMOVE_REQUIRED : Stickiness.AUTO_REMOVE_ENABLED;
        return define(containerId, stickiness, parametry.commandType());
    }

    static ContainerInfo define(String containerId, Stickiness stickiness, ContainerParametry.CommandType commandType) {
        return new ContainerInfoImpl(containerId, stickiness, commandType);
    }

    String id();

    boolean isAutoRemoveEnabled();

    boolean isStopRequired();

    enum Stickiness {
        AUTO_REMOVE_ENABLED,
        MANUAL_REMOVE_REQUIRED
    }

}
