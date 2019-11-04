package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SyncDockerCmd;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class ManualContainerMonitorTest {

    @Test
    public void removeAll() throws ContainmentException {
        DockerClient client = EasyMock.createMock(DockerClient.class);
        UnitTestManualContainerMonitor monitor = new UnitTestManualContainerMonitor();
        Random random = new Random("ManualContainerMonitorTest.removeAll_hanging".hashCode());
        ContainerParametry p = ContainerParametry.builder("oogabooga:latest").build();
        RunnableContainer c = new UnitTestContainerCreator(monitor, random).create(p);
        monitor.removeAll(client, (id, e) -> {
            Assert.fail(id + " " + e);
        });
        assertEquals(1, monitor.commandsExecuted.size());
    }

    @Test
    public void stopAll() throws ContainmentException {
        DockerClient client = EasyMock.createMock(DockerClient.class);
        UnitTestManualContainerMonitor monitor = new UnitTestManualContainerMonitor();
        Random random = new Random("ManualContainerMonitorTest.removeAll_hanging".hashCode());
        ContainerParametry p = ContainerParametry.builder("oogabooga:latest").build();
        RunnableContainer c = new UnitTestContainerCreator(monitor, random).create(p);
        monitor.stopAll(client, (id, e) -> {
            Assert.fail(id + " " + e);
        });
        assertEquals(0, monitor.commandsExecuted.size());
        RunningContainer running = c.start();
        monitor.stopAll(client, (id, e) -> {
            Assert.fail(id + " " + e);
        });
        assertEquals(1, monitor.commandsExecuted.size());
        monitor.removeAll(client, (id, e) -> {
            Assert.fail(id + " " + e);
        });
        assertEquals(2, monitor.commandsExecuted.size());
    }

    private static class UnitTestManualContainerMonitor extends ManualContainerMonitor {

        public List<Object> commandsExecuted = new ArrayList<>();

        public UnitTestManualContainerMonitor() {
            super();
        }

        @Override
        protected <T> T execSyncCommand(DockerClient client, Function<DockerClient, SyncDockerCmd<T>> command) {
            commandsExecuted.add(command);
            return null;
        }
    }

}