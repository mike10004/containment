package io.github.mike10004.containment.lifecycle;

import com.google.common.annotations.VisibleForTesting;

public class RequirementlessLifecycleStage<R, D> implements LifecycleStage<R, D> {

    private final Lifecycle<D> self;

    public RequirementlessLifecycleStage(Lifecycle<D> self) {
        this.self = self;
    }

    @Override
    public D commission(R requirement) throws Exception {
        return self.commission();
    }

    @Override
    public void decommission() {
        self.decommission();
    }

    @VisibleForTesting
    boolean isDelegatingTo(Lifecycle<?> other) {
        return self.equals(other);
    }

    @Override
    public String toString() {
        return String.format("LifecycleStage{%s}", self);
    }
}
