package io.github.mike10004.containment;

import java.util.Objects;

public class ContainerInfoImpl implements ContainerInfo {
    private final String containerId;

    public ContainerInfoImpl(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public String id() {
        return containerId;
    }

    @Override
    public String toString() {
        return String.format("ContainerInfo{id=%s}", containerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContainerInfoImpl)) return false;
        ContainerInfoImpl that = (ContainerInfoImpl) o;
        return Objects.equals(containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerId);
    }
}
