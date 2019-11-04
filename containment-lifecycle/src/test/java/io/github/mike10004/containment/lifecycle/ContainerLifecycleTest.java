package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerMonitor;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.DefaultDockerManager;
import io.github.mike10004.containment.DjContainerCreator;
import io.github.mike10004.containment.DockerClientBuilder;
import io.github.mike10004.containment.DockerExecutor;
import io.github.mike10004.containment.DockerManager;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.ShutdownHookContainerMonitor;
import io.github.mike10004.containment.Uuids;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

public class ContainerLifecycleTest {

    @Test
    public void create() throws Exception {
        Random random = new Random();
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ContainerMonitor monitor = new ShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance().build());
        DockerManager dockerManager = new DefaultDockerManager(config, monitor);
        ContainerParametry parametry = ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
        ContainerLifecycle lifecycle = ContainerLifecycle.create(() -> new DjContainerCreator(dockerManager), parametry, Collections.emptyList());
        RunningContainer container = lifecycle.commission();
        DockerSubprocessResult<String> result;
        try {
            String text = Uuids.randomUuidString(random);
            result = DockerExecutor.create(container.info().id(), StandardCharsets.UTF_8).execute("echo", text);
            assertEquals("exit code", 0, result.exitCode());
            assertEquals("text", text, result.stdout().trim());
            System.out.println("container running OK");
        } finally {
            lifecycle.decommission();
        }
    }
}