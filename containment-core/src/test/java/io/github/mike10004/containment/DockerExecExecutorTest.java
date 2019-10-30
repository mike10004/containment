package io.github.mike10004.containment;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class DockerExecExecutorTest {

    @Test
    public void execute_echo() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder("busybox")
                .command(Arrays.asList("tail", "-f", "/dev/null"))
                .build();
        DockerSubprocessResult<String> result;
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
                DockerExecutor executor = new DockerExecExecutor(container.id(), new HashMap<>(), UTF_8);
                result = executor.execute("echo", "hello, world");
            }
        }
        System.out.println(result);
        assertEquals("result content", "hello, world", result.stdout().trim());
        assertEquals("process exit code", 0, result.exitCode());
    }


}