package io.github.mike10004.containment;

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
        public void execute(ContainerAction preStartAction) throws ContainmentException {
            System.out.format("%s.execute(%s)%n", getClass().getSimpleName(), preStartAction);
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

        @Override
        public void execute(ContainerAction action) throws ContainmentException {
            action.perform(info);
        }

        @Override
        public List<ContainerPort> fetchPorts() throws ContainmentException {
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

}
