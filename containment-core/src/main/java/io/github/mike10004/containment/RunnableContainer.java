package io.github.mike10004.containment;

public interface RunnableContainer {
    CreatedContainer info();
    RunnableContainer prepare(PreStartAction preStartAction);
    RunningContainer start() throws ContainmentException;
}
