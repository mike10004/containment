package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.StartedContainer;

public interface ContainerPostStartRunnable {

    void perform(StartedContainer container) throws Exception;
    default ContainerInitialPostStartAction<Void> asInitialAction() {
        ContainerPostStartRunnable self = this;
        return container -> {
            self.perform(container);
            return (Void) null;
        };
    }

    default <T> ContainerPostStartAction<T, T> asPassThru() {
        ContainerPostStartRunnable self = this;
        return (container, requirement) -> {
            self.perform(container);
            return requirement;
        };
    }
}
