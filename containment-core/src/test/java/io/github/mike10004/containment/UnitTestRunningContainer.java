package io.github.mike10004.containment;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

class UnitTestRunningContainer implements RunningContainer {

    private final ContainerInfo info;

    public UnitTestRunningContainer(ContainerInfo info) {
        this.info = info;
    }

    @Override
    public ContainerInfo info() {
        return info;
    }

    @Override
    public void close() throws ContainmentException {
    }

    @Override
    public List<PortMapping> fetchPorts() throws ContainmentException {
        return Collections.emptyList();
    }

    @Override
    public <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException {
        return consumer;
    }

    @Override
    public <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException {
        return consumer;
    }
}
