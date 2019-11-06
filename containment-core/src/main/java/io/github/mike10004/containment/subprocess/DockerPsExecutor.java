package io.github.mike10004.containment.subprocess;

import com.google.common.base.Verify;
import com.google.common.io.CharSource;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DockerPsExecutor extends DockerSubprocessExecutorBase {

    public DockerPsExecutor() {
        this(emptySubprocessConfig());
    }

    public DockerPsExecutor(SubprocessConfig subprocessConfig) {
        super(subprocessConfig);
    }

    public String describeProcess(String containerId) throws ContainmentException {
        requireNonNull(containerId);
        List<String> lines = listProcesses("id=" + containerId, Function.identity());
        if (lines.isEmpty()) {
            throw new ContainmentException("ps output empty; container does not exist? " + StringUtils.abbreviate(containerId, 256));
        }
        Verify.verify(lines.size() == 1);
        return lines.get(0);
    }

    public <T> List<T> listProcesses(String filter, Function<? super String, ? extends T> transform) throws ContainmentException {
        Subprocess subprocess = buildSubprocessRunningDocker()
                .arg("ps")
                .arg("--filter=" + filter)
                .arg("--format={{json .}}")
                .build();
        ContainerSubprocessResult<String> result = executeDockerSubprocess(subprocess, Charset.defaultCharset());
        if (result.exitCode() != 0) {
            throw new ContainmentException(String.format("nonzero exit code from docker subprocess: %s", result));
        }
        try {
            return CharSource.wrap(result.stdout()).readLines().stream().map(transform).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("unexpected I/O exception reading in-memory string", e);
        }
    }
}
