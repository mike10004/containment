package io.github.mike10004.containment;

public interface RunnableContainer extends AutoCloseable {
    ContainerInfo info();
    void execute(PreStartAction preStartAction) throws ContainmentException;
    RunningContainer start() throws ContainmentException;
    void close() throws ContainmentException;
}
