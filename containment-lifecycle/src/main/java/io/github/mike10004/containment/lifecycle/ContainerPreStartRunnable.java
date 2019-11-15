package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

public interface ContainerPreStartRunnable {

    void perform(ActionableContainer container) throws Exception;

    default ContainerInitialPreStartAction<Void> asInitialAction() {
        ContainerPreStartRunnable self = this;
        return container -> {
            self.perform(container);
            return (Void) null;
        };
    }

    default <T> ContainerPreStartAction<T, T> asPassThru() {
        ContainerPreStartRunnable self = this;
        return (container, requirement) -> {
            self.perform(container);
            return requirement;
        };
    }
}
