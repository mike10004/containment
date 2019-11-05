package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.RunningContainer;

public interface RunningContainerAction {
    void perform(RunningContainer container) throws ContainmentException;
}
