package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

public interface ContainerInitialPreStartAction<P> {
    P perform(ActionableContainer container) throws Exception;
}
