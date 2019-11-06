package io.github.mike10004.containment;

/**
 * Interface of a service that supports launching processes inside started containers.
 */
public interface DockerExecutor {

    /**
     * Launches a process inside a container.
     * @param executable the executable of the process
     * @param args arguments to the executable
     * @return process result
     * @throws ContainmentException
     */
    DockerSubprocessResult<String> execute(String executable, String... args) throws ContainmentException;

}
