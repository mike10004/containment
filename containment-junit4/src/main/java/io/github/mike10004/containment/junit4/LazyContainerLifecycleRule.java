package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.lifecycle.LifecycledDependency;

public class LazyContainerLifecycleRule implements RuleDelegate {

    private final LifecycledDependency<RunningContainer> containerDependency;

    public LazyContainerLifecycleRule(LifecycledDependency<RunningContainer> containerDependency) {
        this.containerDependency = containerDependency;
    }

    @Override
    public void before() throws Throwable {
    }

    @Override
    public void after() {
        containerDependency.finishLifecycle();
    }

    public RunningContainer container() {
        return containerDependency.provide().require();
    }
}
