package io.github.mike10004.containment;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     * Interface that represents the source of an input stream supplying the bytes of a tar file.
     */
    interface TarSource {

        /**
         * Opens the stream.
         * @return the open stream
         * @throws IOException on I/O error
         */
        InputStream open() throws IOException;
    }

    /**
     * Unpacks contents of a tar archive to a directory within the container filesystem.
     * @param source tar archive data source
     * @param destination directory that is to be the parent of the unpacked contents
     * @throws IOException on I/O error
     * @throws ContainmentException on container error
     */
    void unpackTarArchiveToContainer(TarSource source, String destination) throws IOException, ContainmentException;

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
