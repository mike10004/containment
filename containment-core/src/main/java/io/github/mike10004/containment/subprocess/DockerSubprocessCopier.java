package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.ContainerCopier;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.StreamContent;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.SubprocessException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Copier implementation that launches external {@code docker} subprocesses
 * to execute copy commands.
 */
public class DockerSubprocessCopier implements ContainerCopier {

    private final String containerId;
    private final long timeout;
    private TimeUnit timeoutUnit;

    public DockerSubprocessCopier(String containerId) {
        this(containerId, 30, TimeUnit.SECONDS);
    }

    public DockerSubprocessCopier(String containerId, long timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.containerId = requireNonNull(containerId);
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public void copyToContainer(File filepath, String destpath) throws IOException {
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
    public void copyFromContainer(String path, File destinationFile, Set<Option> options) throws IOException {
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

}
