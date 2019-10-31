package io.github.mike10004.containment;

public interface PreStartAction {
    void perform(CreatedContainer unstartedContainer) throws ContainmentException;
}
