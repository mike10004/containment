package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

public interface PreStartContainerStageOne<P> extends PreStartContainerStage<Void, P> {

    @Override
    default P perform(ActionableContainer container, Void requirement) throws Exception {
        return perform(container);
    }

    P perform(ActionableContainer container) throws Exception;
}
