package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

public interface PostStartContainerStageOne<P> extends PostStartContainerStage<Void, P> {

    @Override
    default P perform(StartedContainer container, Void requirement) throws Exception {
        return perform(container);
    }

    P perform(StartedContainer container) throws Exception;
}
