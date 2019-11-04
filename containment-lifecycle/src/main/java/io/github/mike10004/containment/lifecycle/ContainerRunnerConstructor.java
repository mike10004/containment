package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerRunner;
import io.github.mike10004.containment.ContainmentException;

public interface ContainerRunnerConstructor {
    ContainerRunner instantiate() throws ContainmentException;
}
