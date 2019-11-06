package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartedContainer;

public interface RunningContainerAction {
    void perform(StartedContainer container) throws ContainmentException;
}
