package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.lifecycle.LifecycledDependency;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.Objects.requireNonNull;

public class ContainerDependencyRule implements TestRule {

    LifecycledDependency<RunningContainer> containerDependency;

    public ContainerDependencyRule(LifecycledDependency<RunningContainer> containerDependency) {
        this.containerDependency = requireNonNull(containerDependency);
    }

    private void before() {
        // no op; dependency is lazy
    }

    @Override
    public final Statement apply(Statement base, Description description) {
        // copied from org.junit.rules.ExternalResource
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }


    public RunningContainer container() {
        return containerDependency.provide().require();
    }

    private void after() {
        containerDependency.finishLifecycle();
    }
}

