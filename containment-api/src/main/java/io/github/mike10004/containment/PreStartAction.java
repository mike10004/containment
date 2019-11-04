package io.github.mike10004.containment;

public interface PreStartAction {
    void perform(ContainerInfo unstartedContainer) throws ContainmentException;
}
