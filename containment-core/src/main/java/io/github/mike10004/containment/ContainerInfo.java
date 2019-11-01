package io.github.mike10004.containment;

public interface ContainerInfo {

    static ContainerInfo define(String containerId) {
        return new ContainerInfoImpl(containerId);
    }

    String id();

}
