package io.github.mike10004.containment;

public interface ContainerRunner extends AutoCloseable {

    RunnableContainer create(ContainerParametry parametry) throws ContainmentException;

    default RunningContainer run(ContainerParametry parametry) throws ContainmentException {
        try (RunnableContainer createdContainer = create(parametry)) {
            return createdContainer.start();
        }
    }


    @Override
    void close() throws ContainmentException;

}
