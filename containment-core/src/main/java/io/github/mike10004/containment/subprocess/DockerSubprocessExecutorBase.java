package io.github.mike10004.containment.subprocess;

import com.google.common.base.MoreObjects;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;

import java.nio.charset.Charset;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

public class DockerSubprocessExecutorBase {

    private final SubprocessConfig subprocessConfig;

    public DockerSubprocessExecutorBase(SubprocessConfig subprocessConfig) {
        this.subprocessConfig = requireNonNull(subprocessConfig);
    }

    protected final Subprocess.Builder buildSubprocessRunningDocker() {
        String dockerExecutable = subprocessConfig.apply("docker.subprocess.executable", "docker");
        return Subprocess.running(dockerExecutable);
    }

    public static SubprocessConfig emptySubprocessConfig() {
        return (x, y) -> y;
    }

    protected DockerSubprocessResult<String> executeDockerSubprocess(Subprocess subprocess, Charset execOutputCharset) throws ContainmentException {
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            ProcessMonitor<String, String> monitor = subprocess.launcher(processTracker)
                    .outputStrings(execOutputCharset).launch();
            ProcessResult<String, String> result = monitor.await();
            return new SubprocessExecResult<>(result);
        } catch (InterruptedException e) {
            throw new ContainmentException(e);
        }
    }

    /**
     * Interface of a service that provides subprocess configuration settings.
     */
    public interface SubprocessConfig extends BiFunction<String, String, String> {

        /**
         * Gets the value of a setting.
         * @param key
         * @param defaultValue
         * @return setting value, or default if not defined
         */
        @Override
        String apply(String key, String defaultValue);

    }

    private static class SubprocessExecResult<T> implements DockerSubprocessResult<T> {

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
