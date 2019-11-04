package io.github.mike10004.containment;

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
        DockerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             RunnableContainer runnable = runner.create(parametry)) {
            try (RunningContainer container = runnable.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), new HashMap<>(), UTF_8);
                result = executor.execute("echo", "hello, world");
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

        DockerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             RunnableContainer runnable = runner.create(parametry)) {
            try (RunningContainer container = runnable.start()) {
                Map<String, String> processEnvironment = new HashMap<>();
                processEnvironment.put("FOO", "bar");
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), processEnvironment, UTF_8);
                result = executor.execute("printenv");
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        Tests.assertStdoutHasLine(result, "FOO=bar");
    }

}