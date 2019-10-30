package io.github.mike10004.containment;

import java.nio.charset.Charset;
import java.util.Map;

public interface DockerExecutor {

    DockerExecResult<String> execute(String executable, String... args) throws ContainmentException;

    static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables) {
        return create(containerId, executionEnvironmentVariables, Charset.defaultCharset());
    }

    static DockerExecutor create(String containerId, Map<String, String> executionEnvironmentVariables, Charset charset) {
        return new DockerSubprocessExecutor(containerId, executionEnvironmentVariables, charset);
    }

}
