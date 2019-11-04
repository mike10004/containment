package io.github.mike10004.containment;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public interface DockerExecutor {

    DockerSubprocessResult<String> execute(String executable, String... args) throws ContainmentException;

    static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables) {
        return create(containerId, executionEnvironmentVariables, Charset.defaultCharset());
    }

    static DockerExecutor create(String containerId, Charset charset) {
        return create(containerId, Collections.emptyMap(), charset);
    }

    static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables, Charset charset) {
        return new DockerExecExecutor(containerId, executionEnvironmentVariables, charset);
    }

}
