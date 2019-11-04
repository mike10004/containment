package io.github.mike10004.containment;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public interface DockerExecutor {

    DockerSubprocessResult<String> execute(String executable, String... args) throws ContainmentException;

}
