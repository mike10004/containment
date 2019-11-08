package io.github.mike10004.containment.lifecycle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a lifecycle that composes multiple sequential lifecycles.
 * @param <T> type of top element of the stack (last created, first destroyed)
 */
public class LifecycleStack<T> implements Lifecycle<T> {

    private final Iterable<? extends Lifecycle<?>> preliminaryStages;
    private final Lifecycle<T> finalStage;
    private transient final Deque<Lifecycle<?>> commissioned;

    /**
     * Constructs a new instance.
     * @param preliminaryStages preliminary lifecycles
     * @param finalStage lifecycle that produces the instance that this stack commissions
     */
    public LifecycleStack(Iterable<? extends Lifecycle<?>> preliminaryStages, Lifecycle<T> finalStage) {
        commissioned = new ArrayDeque<>();
        this.finalStage = requireNonNull(finalStage);
        this.preliminaryStages = requireNonNull(preliminaryStages);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Lifecycle<?>> preliminaries;

        private Builder() {
            preliminaries = new ArrayList<>();
        }

        public <T> LifecycleStack<T> finish(Lifecycle<T> finalStage) {
            return new LifecycleStack<>(preliminaries, finalStage);
        }

        public Builder addStage(Lifecycle<?> stage) {
            preliminaries.add(stage);
            return this;
        }
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", LifecycleStack.class.getSimpleName() + "[", "]")
                .add("others=" + preliminaryStages)
                .add("top=" + finalStage)
                .add("commissioned.size=" + commissioned.size())
                .toString();
    }

    private void unwind() throws LifecycleStackDecommissionException {
        Map<Lifecycle<?>, RuntimeException> exceptionsThrown = new LinkedHashMap<>();
        while (!commissioned.isEmpty()) {
            Lifecycle<?> lifecycle = commissioned.pop();
            try {
                lifecycle.decommission();
            } catch (RuntimeException e) {
                exceptionsThrown.put(lifecycle, e);
            }
        }
        if (!exceptionsThrown.isEmpty()) {
            throw new LifecycleStackDecommissionException(exceptionsThrown);
        }
    }

    /**
     * Performs the commission stage of each lifecycle in this instance's sequence.
     * If commissioning any lifecycle in the sequence fails, then
     * those already commissioned are decommissioned before throwing
     * the exception that caused the commissioning failure.
     * @return the final commissioned resource
     * @throws LifecycleStackCommissionException on error
     */
    @Override
    public T commission() throws LifecycleStackCommissionException {
        Lifecycle<?> thrower = null;
        Exception throwable = null;
        for (Lifecycle<?> lifecycle : preliminaryStages) {
            try {
                lifecycle.commission();
                commissioned.push(lifecycle);
            } catch (Exception e) {
                thrower = lifecycle;
                throwable = e;
            }
        }
        if (throwable == null) {
            try {
                T top = this.finalStage.commission();
                commissioned.push(this.finalStage);
                return top;
            } catch (Exception e) {
                thrower = finalStage;
                throwable = e;
            }
        }
        LifecycleStackDecommissionException unwindException = null;
        try {
            unwind();
        } catch (LifecycleStackDecommissionException e) {
            unwindException = e;
        }
        if (unwindException == null) {
            throw new LifecycleStackCommissionException(throwable);
        } else {
            throw new LifecycleStackCommissionUnwindException(thrower, throwable, unwindException);
        }
    }

    /**
     * Decommissions each commissioned lifecycle, starting with the most recent and
     * going back to the first.
     */
    @Override
    public void decommission() {
        unwind();
    }

}
