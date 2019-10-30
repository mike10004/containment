package io.github.mike10004.containment;

import com.google.common.collect.ImmutableMap;
import io.github.mike10004.subprocess.Subprocess;

import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of an executor that launches an external {@code docker exec} process.
 */
class DockerExecExecutor extends DockerSubprocessExecutorBase implements DockerExecutor {

    private final String containerId;
    private final Map<String, String> containerProcessEnvironment;
    private final Charset execOutputCharset;

    public DockerExecExecutor(String containerId, Map<String, String> containerProcessEnvironment, Charset execOutputCharset) {
        this(containerId, containerProcessEnvironment, execOutputCharset, emptySubprocessConfig());
    }

    public DockerExecExecutor(String containerId, Map<String, String> containerProcessEnvironment, Charset execOutputCharset, SubprocessConfig subprocessConfig) {
        super(subprocessConfig);
        this.containerId = requireNonNull(containerId, "containerId");
        this.containerProcessEnvironment = ImmutableMap.copyOf(containerProcessEnvironment);
        this.execOutputCharset = requireNonNull(execOutputCharset);
    }

    @Override
    public DockerSubprocessResult<String> execute(String executable, String... args) throws ContainmentException {
        Subprocess.Builder b = buildSubprocessRunningDocker().arg("exec");
        containerProcessEnvironment.forEach((name, value) -> {
            b.arg("--env").arg(String.format("%s=%s", name, value));
        });
        Subprocess subprocess = b.arg(containerId)
                                 .args(executable, args).build();
        return executeDockerSubprocess(subprocess, execOutputCharset);
    }

}
