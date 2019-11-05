package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.containment.subprocess.DockerExecExecutor;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class Common {
    private Common() {}

    public static void assertContainerAlive(ContainerInfo info) throws ContainmentException {
        DockerSubprocessResult<String> result = new DockerExecExecutor(info.id(), Collections.emptyMap(), UTF_8).execute("printenv");
        System.out.println(result.stdout());
        assertEquals("exit code", 0, result.exitCode());

    }
}
