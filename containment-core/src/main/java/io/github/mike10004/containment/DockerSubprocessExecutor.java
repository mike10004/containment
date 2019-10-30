package io.github.mike10004.containment;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;

import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of an executor that launches an external {@code docker exec} process.
 */
class DockerSubprocessExecutor implements DockerExecutor {

    private final String containerId;
    private final Map<String, String> executionEnvironmentVariables;
    private final Charset execOutputCharset;

    public DockerSubprocessExecutor(String containerId, Map<String, String> executionEnvironmentVariables, Charset execOutputCharset) {
        this.containerId = requireNonNull(containerId, "containerId");
        this.executionEnvironmentVariables = ImmutableMap.copyOf(executionEnvironmentVariables);
        this.execOutputCharset = requireNonNull(execOutputCharset);
    }

    @Override
    public DockerExecResult<String> execute(String executable, String... args) throws ContainmentException {
        Subprocess.Builder b = Subprocess.running("docker");
        b.arg("exec");
        executionEnvironmentVariables.forEach((name, value) -> {
            b.arg("--env").arg(String.format("%s=%s", name, value));
        });
        Subprocess subprocess = b.arg(containerId)
                                 .args(executable, args).build();
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            ProcessMonitor<String, String> monitor = subprocess.launcher(processTracker)
                    .outputStrings(execOutputCharset).launch();
            ProcessResult<String, String> result = monitor.await();
            return new SubprocessExecResult<>(result);
        } catch (InterruptedException e) {
            throw new ContainmentException(e);
        }
    }

    private static class SubprocessExecResult<T> implements DockerExecResult<T> {

        private final ProcessResult<T, T> result;

        private SubprocessExecResult(ProcessResult<T, T> result) {
            this.result = requireNonNull(result);
        }

        @Override
        public int exitCode() {
            return result.exitCode();
        }

        @Override
        public T stdout() {
            return result.content().stdout();
        }

        @Override
        public T stderr() {
            return result.content().stderr();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("SubprocessExecResult")
                    .addValue(result)
                    .toString();
        }

    }
}
