package io.github.mike10004.containment.subprocess;

import com.google.common.collect.ImmutableMap;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.DockerExecutor;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.subprocess.Subprocess;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of an executor that launches an external {@code docker exec} process.
 */
public class DockerExecExecutor extends DockerSubprocessExecutorBase implements DockerExecutor {

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

    public static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables) {
        return create(containerId, executionEnvironmentVariables, Charset.defaultCharset());
    }

    public static DockerExecutor create(String containerId, Charset charset) {
        return create(containerId, Collections.emptyMap(), charset);
    }

    public static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables, Charset charset) {
        return new DockerExecExecutor(containerId, executionEnvironmentVariables, charset);
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
