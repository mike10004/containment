package io.github.mike10004.containment;

import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.StreamContent;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.SubprocessException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Copier implementation that launches external {@code docker} subprocesses
 * to execute copy commands.
 */
public class DockerSubprocessCopier implements DockerCopier {

    private final long timeout;
    private TimeUnit timeoutUnit;

    public DockerSubprocessCopier() {
        this(30, TimeUnit.SECONDS);
    }

    public DockerSubprocessCopier(long timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public void copyToContainer(File filepath, String containerId, String destpath) throws IOException {
        Subprocess subprocess = Subprocess.running("docker")
                .arg("cp")
                .arg(filepath.getAbsolutePath())
                .arg(String.format("%s:%s", containerId, destpath))
                .build();
        execute(subprocess);
    }

    protected void execute(Subprocess subprocess) throws IOException, SubprocessException {
        ProcessResult<?, ?> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .inheritOutputStreams()
                    .launch()
                    .await(timeout, timeoutUnit);
        } catch (InterruptedException | TimeoutException e) {
            throw new DockerCopyException(e);
        }
        if (result.exitCode() != 0) {
            throw new DockerCopyFailedException(result);
        }
    }

    @Override
    public void copyFromContainer(String containerId, String path, File destinationFile, Set<Option> options) throws IOException {
        if (path.isEmpty()) {
            throw new DockerCopyException("source path is empty");
        }
        Subprocess.Builder b = Subprocess.running("docker")
                .arg("cp");
        for (Option option : options) {
            switch (option) {
                case ARCHIVE:
                    b.arg("--archive");
                    break;
                case FOLLOW_LINK:
                    b.arg("--follow-link");
                    break;
                default:
                    throw new DockerCopyException("unsupported option: " + option);
            }
        }
        Subprocess subprocess = b.arg(String.format("%s:%s", containerId, path))
                .arg(destinationFile.getAbsolutePath())
                .build();
        execute(subprocess);
    }

    private static class DockerCopyFailedException extends DockerCopyException {
        public DockerCopyFailedException(ProcessResult<?, ?> result) {
            super(createMessage(result));
        }

        private static String createMessage(ProcessResult<?, ?> result) {
            int exitCode = result.exitCode();
            StringBuilder sb = new StringBuilder(256);
            sb.append("exit code ").append(exitCode);
            StreamContent<?, ?> content = result.content();
            appendIfNonNull("stdout", content.stdout(), sb);
            appendIfNonNull("stderr", content.stderr(), sb);
            return sb.toString();
        }

        private static void appendIfNonNull(String tag, @Nullable Object value, StringBuilder sb) {
            if (value != null) {
                sb.append("; ").append(tag).append(value);
            }
        }
    }

    public static DockerCopier create() {
        return new DockerSubprocessCopier();
    }

    public static DockerCopier create(Duration timeout) {
        return new DockerSubprocessCopier(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
