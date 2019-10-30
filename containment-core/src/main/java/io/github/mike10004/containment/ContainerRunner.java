package io.github.mike10004.containment;

public interface ContainerRunner extends AutoCloseable {

    RunnableContainer create(ContainerParametry parametry) throws ContainmentException;

    default RunningContainer run(ContainerParametry parametry) throws ContainmentException {
        return create(parametry).start();
    }


    @Override
    void close() throws ContainmentException;

}
