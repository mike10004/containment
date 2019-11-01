package io.github.mike10004.containment;

import java.util.Random;
import java.util.function.Consumer;

class UnitTestContainerRunner implements ContainerRunner {

    private final Random random;

    UnitTestContainerRunner(Random random) {
        this.random = random;
    }

    @Override
    public RunnableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException {
        return new UnitTestRunnableContainer(ContainerInfo.define(Uuids.randomUuidString(random)));
    }

    @Override
    public void close() throws ContainmentException {
    }
}
