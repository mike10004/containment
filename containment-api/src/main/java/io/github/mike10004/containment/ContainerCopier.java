package io.github.mike10004.containment;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

/**
 * Interface of a service that copies files to and from a Docker container.
 */
public interface ContainerCopier {

    /**
     * Enumeration of copy options.
     */
    enum Option {

        /**
         * Archive mode (copy all uid/gid information).
         */
        ARCHIVE,

        /**
         * Always follow symbol link in source path.
         */
        FOLLOW_LINK
    }

    /**
     * Copies a file from the local filesystem to the container filesystem.
     * @param sourceFile the source file
     * @param path the destination path within the container; it it's an existing directory, and the full destination
     * pathname will have the same filename as the source file and the given path as the parent
     * @throws IOException
     */
    void copyToContainer(File sourceFile, String path) throws IOException, ContainmentException;

    /**
     * Copies data to the container filesystem.
     * @param sourceBytes data to copy
     * @param tmpDir a temp dir on the host local filesystem
     * @param destination destination within the container filesystem
     * @throws IOException on I/O error
     * @throws ContainmentException on container error
     */
    default void copyToContainer(byte[] sourceBytes, File tmpDir, String destination)  throws IOException, ContainmentException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("pre-start-action", ".tmp", tmpDir);
            java.nio.file.Files.write(tempFile.toPath(), sourceBytes, StandardOpenOption.WRITE);
            copyToContainer(tempFile, destination);
        } finally {
            if (tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }

    }

    /**
     * Copies a file from the container filesystem to the local filesystem.
     * @param path the path of the source file within the container filesystem
     * @param destinationFile the destination path on the local filesystem; must be a file pathname, not a directory
     * @param options copy options; depending on the implementation, these may not have any effect
     * @throws IOException
     */
    void copyFromContainer(String path, File destinationFile, Set<Option> options) throws IOException, ContainmentException;

    /**
     * Copies a file from the container filesystem to the local filesystem.
     * @param path the path of the source file within the container filesystem
     * @param destinationFile the destination path on the local filesystem; must be a file pathname, not a directory
     * @param firstOption a copy option
     * @param otherOptions more copy options
     * @throws IOException
     * @see #copyFromContainer(String, File, Set)
     */
    default void copyFromContainer(String path, File destinationFile, Option firstOption, Option...otherOptions) throws IOException, ContainmentException {
        copyFromContainer(path, destinationFile, EnumSet.copyOf(Lists.asList(firstOption, otherOptions)));
    }

    default void copyFromContainer(String path, File destinationFile) throws IOException, ContainmentException {
        copyFromContainer(path, destinationFile, EnumSet.noneOf(Option.class));
    }

    class DockerCopyException extends IOException {
        public DockerCopyException(String message) {
            super(message);
        }
        public DockerCopyException(Throwable cause) {
            super(cause);
        }
    }

}
