package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainmentException;

public interface ContainerRunnerConstructor {
    ContainerCreator instantiate() throws ContainmentException;
}
