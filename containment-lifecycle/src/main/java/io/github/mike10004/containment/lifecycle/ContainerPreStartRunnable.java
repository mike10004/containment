package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;

public interface ContainerPreStartRunnable {

    void perform(ActionableContainer container) throws Exception;

}
