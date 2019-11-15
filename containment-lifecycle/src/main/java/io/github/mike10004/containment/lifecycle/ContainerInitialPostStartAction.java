package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

public interface ContainerInitialPostStartAction<P> {

    P perform(StartedContainer container) throws Exception;

}
