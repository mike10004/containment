package io.github.mike10004.containment;

import java.util.List;
import java.util.function.Consumer;

public interface RunningContainer extends AutoCloseable {

    ContainerInfo info();

    @Override
    void close() throws ContainmentException;

    List<PortMapping> fetchPorts() throws ContainmentException;

    <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException;

    <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException;

}
