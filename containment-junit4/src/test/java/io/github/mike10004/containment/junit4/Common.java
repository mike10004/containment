package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.subprocess.DockerExecExecutor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class Common {
    private Common() {}

    public static void assertContainerAlive(ContainerInfo info) throws ContainmentException {
        ContainerSubprocessResult<String> result = new DockerExecExecutor(info.id()).execute(UTF_8,"true");
        assertEquals("exit code", 0, result.exitCode());
    }
}
