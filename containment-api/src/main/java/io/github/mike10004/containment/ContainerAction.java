package io.github.mike10004.containment;

public interface ContainerAction {
    void perform(ContainerInfo container) throws ContainmentException;
}
