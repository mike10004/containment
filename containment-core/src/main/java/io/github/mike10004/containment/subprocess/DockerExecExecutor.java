package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.ContainerExecutor;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.subprocess.Subprocess;

import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of an executor that launches an external {@code docker exec} process.
 */
public class DockerExecExecutor extends DockerSubprocessExecutorBase implements ContainerExecutor {

    private final String containerId;

    public DockerExecExecutor(String containerId) {
        this(containerId, emptySubprocessConfig());
    }

    public DockerExecExecutor(String containerId, SubprocessConfig subprocessConfig) {
        super(subprocessConfig);
        this.containerId = requireNonNull(containerId, "containerId");
    }

    @Override
    public ContainerSubprocessResult<String> execute(Map<String, String> containerProcessEnvironment, Charset execOutputCharset, String executable, String... args) throws ContainmentException {
        Subprocess.Builder b = buildSubprocessRunningDocker().arg("exec");
        containerProcessEnvironment.forEach((name, value) -> {
            b.arg("--env").arg(String.format("%s=%s", name, value));
        });
        Subprocess subprocess = b.arg(containerId)
                                 .args(executable, args).build();
        return executeDockerSubprocess(subprocess, execOutputCharset);
    }

}
