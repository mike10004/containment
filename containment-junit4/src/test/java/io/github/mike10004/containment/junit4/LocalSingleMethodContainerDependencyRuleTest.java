package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.containment.subprocess.DockerExecExecutor;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class LocalSingleMethodContainerDependencyRuleTest {

    @Rule
    public ContainerDependencyRule rule = ContainerDependencyRule.builder(parametry())
            .build();

    private static ContainerParametry parametry() {
        return ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
    }

    @Test
    public void tryIt() throws Exception {
        ContainerInfo info = rule.container().info();
        DockerSubprocessResult<String> result = new DockerExecExecutor(info.id(), Collections.emptyMap(), UTF_8).execute("printenv");
        System.out.println(result.stdout());
        assertEquals("exit code", 0, result.exitCode());
    }
}