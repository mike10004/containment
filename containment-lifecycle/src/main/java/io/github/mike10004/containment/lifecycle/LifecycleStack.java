package io.github.mike10004.containment.lifecycle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Implementation of a lifecycle that composes multiple sequential lifecycles.
 * @param <T> type of top element of the stack (last created, first destroyed)
 */
public class LifecycleStack<T> implements Lifecycle<T> {

    private final Iterable<? extends Lifecycle<?>> others;
    private final Deque<Lifecycle<?>> commissioned;
    private transient final Lifecycle<T> top;

    /**
     * Constructs a new instance.
     * @param others preliminary lifecycles
     * @param top lifecycle that produces the instance that this stack commissions
     */
    public LifecycleStack(Iterable<? extends Lifecycle<?>> others, Lifecycle<T> top) {
        commissioned = new ArrayDeque<>();
        this.top = top;
        this.others = others;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LifecycleStack.class.getSimpleName() + "[", "]")
                .add("others=" + others)
                .add("top=" + top)
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
     * Commissions each lifecycle in sequence.
     * If commissioning any lifecycle in the sequence fails, then
     * those already commissioned are decommissioned before throwing
     * the exception that
     * @return the final commissioned object
     * @throws Exception on error
     */
    @Override
    public T commission() throws LifecycleStackCommissionException {
        Lifecycle<?> thrower = null;
        Exception throwable = null;
        for (Lifecycle<?> lifecycle : others) {
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
                T top = this.top.commission();
                commissioned.push(this.top);
                return top;
            } catch (Exception e) {
                thrower = top;
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
