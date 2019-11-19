package io.github.mike10004.containment;

import static java.util.Objects.requireNonNull;

class ContainerInfoImpl implements ContainerInfo {

    private final String containerId;
    private final Stickiness stickiness;
    private final ContainerParametry.CommandType commandType;

    public ContainerInfoImpl(String containerId, Stickiness stickiness, ContainerParametry.CommandType commandType) {
        this.containerId = requireNonNull(containerId, "containerId");
        this.stickiness = requireNonNull(stickiness);
        this.commandType = requireNonNull(commandType);
    }

    @Override
    public String id() {
        return containerId;
    }

    @Override
    public String toString() {
        return String.format("ContainerInfo{id=%s,%s,%s}", containerId, stickiness, commandType);
    }

    @Override
    public boolean isAutoRemoveEnabled() {
        return stickiness == Stickiness.AUTO_REMOVE_ENABLED;
    }

    @Override
    public boolean isStopRequired() {
        return commandType == ContainerParametry.CommandType.BLOCKING;
    }
}
