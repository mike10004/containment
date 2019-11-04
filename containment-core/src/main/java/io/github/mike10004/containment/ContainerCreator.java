package io.github.mike10004.containment;

import java.util.function.Consumer;

public interface ContainerCreator extends AutoCloseable {

    default RunnableContainer create(ContainerParametry parametry) throws ContainmentException {
        return create(parametry, ignore -> {});
    }

    RunnableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException;

    @Override
    void close() throws ContainmentException;

}
