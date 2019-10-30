package io.github.mike10004.containment;

public interface ContainerRunner extends AutoCloseable {

    RunningContainer run(ContainerParametry parametry);

    @Override
    void close() throws ContainmentException;

}
