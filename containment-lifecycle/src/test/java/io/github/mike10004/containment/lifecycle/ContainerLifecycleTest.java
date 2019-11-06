package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.Uuids;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.subprocess.DockerExecExecutor;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ContainerLifecycleTest {

    @Test
    public void create() throws Exception {
        Random random = new Random();
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DjContainerMonitor monitor = new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance().build());
        DjDockerManager dockerManager = new DefaultDjDockerManager(config, monitor);
        ContainerParametry parametry = ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
        ContainerLifecycle lifecycle = ContainerLifecycle.create(() -> new DjContainerCreator(dockerManager), parametry, Collections.emptyList(), Collections.emptyList());
        StartedContainer container = lifecycle.commission();
        ContainerSubprocessResult<String> result;
        try {
            String text = Uuids.randomUuidString(random);
            result = container.executor().execute(StandardCharsets.UTF_8, "echo", text);
            assertEquals("exit code", 0, result.exitCode());
            assertEquals("text", text, result.stdout().trim());
            System.out.println("container running OK");
        } finally {
            lifecycle.decommission();
        }
    }
}