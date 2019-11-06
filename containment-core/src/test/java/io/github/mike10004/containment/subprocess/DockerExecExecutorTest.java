package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerExecutor;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.core.TestDockerManager;
import io.github.mike10004.containment.core.Tests;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class DockerExecExecutorTest {

    @Test
    public void execute_echo() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForEchoTest())
                .commandToWaitIndefinitely()
                .build();
        ContainerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                ContainerExecutor executor = container.executor();
                result = executor.execute(UTF_8, "echo", "hello, world");
            }
        }
        System.out.println(result);
        assertEquals("result content", "hello, world", result.stdout().trim());
        assertEquals("process exit code", 0, result.exitCode());
    }

    @Test
    public void execute_setProcessEnvironmentVariables() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .commandToWaitIndefinitely()
                .build();

        ContainerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                Map<String, String> processEnvironment = new HashMap<>();
                processEnvironment.put("FOO", "bar");
                ContainerExecutor executor = container.executor();
                result = executor.execute(processEnvironment, UTF_8, "printenv");
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        Tests.assertStdoutHasLine(result, "FOO=bar");
    }

}