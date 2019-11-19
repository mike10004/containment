package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.lifecycle.FirstProvisionFailedException;
import io.github.mike10004.containment.lifecycle.LifecycledResource;
import io.github.mike10004.containment.lifecycle.Provision;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.Objects.requireNonNull;

/**
 * Rule that provides a lazily-started container that is stopped
 * and removed on test teardown.
 */
public class ContainerDependencyRule implements TestRule {

    private final LifecycledResource<StartedContainer> containerResource;

    /**
     * Constructs an instance of the rule.
     * @param containerResource the container resource
     */
    public ContainerDependencyRule(LifecycledResource<StartedContainer> containerResource) {
        this.containerResource = requireNonNull(containerResource);
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

    /**
     * Acquires the started container. If the container has not yet been started,
     * invoking this method will start the container.
     * @return the started container
     * @throws FirstProvisionFailedException if {@link Provision#require()} throws it
     */
    public StartedContainer container() throws FirstProvisionFailedException {
        return containerResource.request().require();
    }

    private void after() {
        containerResource.finishLifecycle();
    }
}

