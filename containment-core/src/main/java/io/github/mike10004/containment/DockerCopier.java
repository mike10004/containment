package io.github.mike10004.containment;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Interface of a service that copies files to and from a Docker container.
 */
public interface DockerCopier {

    enum Option {
        ARCHIVE,
        FOLLOW_LINK
    }

    /**
     * Copies a file from the local filesystem to the container filesystem.
     * @param sourceFile the source file
     * @param containerId the container id
     * @param path the destination path within the container; it it's an existing directory, and the full destination
     * pathname will have the same filename as the source file and the given path as the parent
     * @throws IOException
     */
    void copyToContainer(File sourceFile, String containerId, String path) throws IOException;

    /**
     * Copies a file from the container filesystem to the local filesystem.
     * @param containerId the container id
     * @param path the path of the source file within the container filesystem
     * @param destinationFile the destination path on the local filesystem; must be a file pathname, not a directory
     * @param options copy options
     * @throws IOException
     */
    void copyFromContainer(String containerId, String path, File destinationFile, Set<Option> options) throws IOException;

    default void copyFromContainer(String containerId, String path, File destinationFile, Option firstOption, Option...otherOptions) throws IOException {
        copyFromContainer(containerId, path, destinationFile, EnumSet.copyOf(Lists.asList(firstOption, otherOptions)));
    }

    default void copyFromContainer(String containerId, String path, File destinationFile) throws IOException {
        copyFromContainer(containerId, path, destinationFile, EnumSet.noneOf(Option.class));
    }

    class DockerCopyException extends IOException {
        public DockerCopyException(String message) {
            super(message);
        }
        public DockerCopyException(Throwable cause) {
            super(cause);
        }
    }

    static DockerCopier create() {
        return new DockerSubprocessCopier();
    }

    static DockerCopier create(Duration timeout) {
        return new DockerSubprocessCopier(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
