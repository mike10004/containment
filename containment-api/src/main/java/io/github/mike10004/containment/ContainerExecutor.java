package io.github.mike10004.containment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Interface of a service that supports launching processes inside started containers.
 */
public interface ContainerExecutor {

    /**
     * Launches a process inside a container.
     * @param executable the executable of the process
     * @param args arguments to the executable
     * @return process result
     * @throws ContainmentException
     */
    ContainerSubprocessResult<String> execute(Map<String, String> containerProcessEnvironment, Charset processStreamCharset, String executable, String... args) throws ContainmentException;

    /**
     * @see #execute(Map, Charset, String, String...)
     * @see #defaultExecOutputCharset()
     */
    default ContainerSubprocessResult<String> execute(Map<String, String> containerProcessEnvironment, String executable, String... args) throws ContainmentException {
        return execute(containerProcessEnvironment, defaultExecOutputCharset(), executable, args);
    }

    /**
     * @see #execute(Map, Charset, String, String...)
     * @see #defaultExecOutputCharset()
     */
    default ContainerSubprocessResult<String> execute(Charset processStreamCharset, String executable, String... args) throws ContainmentException {
        return execute(Collections.emptyMap(), processStreamCharset, executable, args);
    }

    /**
     * @see #execute(Map, Charset, String, String...)
     * @see #defaultExecOutputCharset()
     */
    default ContainerSubprocessResult<String> execute(String executable, String... args) throws ContainmentException {
        return execute(Collections.emptyMap(), defaultExecOutputCharset(), executable, args);
    }

    /**
     * Returns the charset used to decode process output, if not otherwise specified.
     * @return a charset
     */
    static Charset defaultExecOutputCharset()  {
        return StandardCharsets.UTF_8;
    }
}
