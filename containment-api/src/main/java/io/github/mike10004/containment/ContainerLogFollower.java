package io.github.mike10004.containment;

import java.util.function.Consumer;

public interface ContainerLogFollower {

    /**
     * Starts following the standard output stream of the container process.
     * This method returns immediately, and the consumer accepts frames of output
     * asynchronously.
     * Frames of the output stream usually contain a single line of text output.
     * @param consumer the consumer
     * @param <C> consumer type
     * @return the argument consumer
     * @throws ContainmentException if container process standard output stream could not be followed
     */
    <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException;

    /**
     * Starts following the standard error stream of the container process.
     * This method returns immediately, and the consumer accepts frames of output
     * asynchronously.
     * Frames of the output stream usually contain a single line of text output.
     * @param consumer the consumer
     * @param <C> consumer type
     * @return the argument consumer
     * @throws ContainmentException if container process standard error stream could not be followed
     */
    <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException;

}
