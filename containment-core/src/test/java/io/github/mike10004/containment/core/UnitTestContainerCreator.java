package io.github.mike10004.containment.core;

import io.github.mike10004.containment.ContainerCopier;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerExecutor;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerInspector;
import io.github.mike10004.containment.ContainerLogFollower;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerPort;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.Uuids;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class UnitTestContainerCreator implements ContainerCreator {

    private final DjContainerMonitor monitor;
    private final Random random;

    public UnitTestContainerCreator(DjContainerMonitor monitor, Random random) {
        this.random = random;
        this.monitor = monitor;
    }

    @Override
    public StartableContainer create(ContainerParametry parametry, Consumer<? super String> warningListener) throws ContainmentException {
        String id = Uuids.randomUuidString(random);
        monitor.created(id);
        return new UnitTestStartableContainer(ContainerInfo.define(id, ContainerInfo.Stickiness.MANUAL_REMOVE_REQUIRED, ContainerParametry.CommandType.BLOCKING));
    }

    @Override
    public void close() throws ContainmentException {
    }

    class UnitTestStartableContainer implements StartableContainer {

        private final ContainerInfo info;

        UnitTestStartableContainer(ContainerInfo info) {
            this.info = info;
        }

        @Override
        public ContainerInfo info() {
            return info;
        }

        @Override
        public StartedContainer start() throws ContainmentException {
            monitor.started(info.id());
            return new UnitTestStartedContainer(info);
        }

        @Override
        public void close() throws ContainmentException {
            monitor.removed(info().id());
        }

        @Override
        public ContainerCopier copier() {
            throw new UnsupportedOperationException("not implemented in this unit test");
        }
    }

    class UnitTestStartedContainer implements StartedContainer {

        private final ContainerInfo info;

        public UnitTestStartedContainer(ContainerInfo info) {
            this.info = info;
        }

        @Override
        public ContainerInfo info() {
            return info;
        }

        @Override
        public void close() throws ContainmentException {
            monitor.stopped(info().id());
        }

        private class Inspector implements ContainerInspector {

            @Override
            public List<ContainerPort> fetchPorts() throws ContainmentException {
                return Collections.emptyList();
            }
        }

        private class LogFollower implements ContainerLogFollower {
            @Override
            public <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException {
                return consumer;
            }

            @Override
            public <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException {
                return consumer;
            }

        }

        @Override
        public ContainerInspector inspector() {
            return new Inspector();
        }

        @Override
        public ContainerLogFollower logs() {
            return new LogFollower();
        }

        @Override
        public ContainerCopier copier() {
            throw new UnsupportedOperationException("not implemented in this unit test");
        }

        @Override
        public ContainerExecutor executor() {
            throw new UnsupportedOperationException("not implemented in this unit test");
        }
    }

}
