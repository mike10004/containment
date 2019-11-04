package io.github.mike10004.containment.lifecycle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @param <T> type of top element of the stack (last created, first destroyed)
 */
public class LifecycleStack<T> implements DependencyLifecycle<T> {

    private final Iterable<? extends DependencyLifecycle<?>> others;
    private final Deque<DependencyLifecycle<?>> commissioned;
    private final DependencyLifecycle<T> top;

    public LifecycleStack(Iterable<? extends DependencyLifecycle<?>> others, DependencyLifecycle<T> top) {
        commissioned = new ArrayDeque<>();
        this.top = top;
        this.others = others;
    }

    private void unwind() {
        Map<DependencyLifecycle<?>, RuntimeException> exceptionsThrown = new LinkedHashMap<>();
        while (!commissioned.isEmpty()) {
            DependencyLifecycle<?> lifecycle = commissioned.pop();
            try {
                lifecycle.decommission();
            } catch (RuntimeException e) {
                exceptionsThrown.put(lifecycle, e);
            }
        }
        if (!exceptionsThrown.isEmpty()) {
            throw new UnwindException(exceptionsThrown);
        }
    }

    @Override
    public T commission() throws Exception {
        DependencyLifecycle<?> thrower = null;
        Exception throwable = null;
        for (DependencyLifecycle<?> lifecycle : others) {
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
        UnwindException unwindException = null;
        try {
            unwind();
        } catch (UnwindException e) {
            unwindException = e;
        }
        if (unwindException == null) {
            throw throwable;
        } else {
            throw new CommissionFailedAndUnwindFailedException(thrower, throwable, unwindException);
        }
    }

    @Override
    public void decommission() {
        unwind();
    }

    static class CommissionFailedAndUnwindFailedException extends Exception {
        public final DependencyLifecycle<?> commissionExceptionThrower;
        public final Exception commissionException;
        public final UnwindException unwindException;

        public CommissionFailedAndUnwindFailedException(DependencyLifecycle<?> commissionExceptionThrower, Exception commissionException, UnwindException unwindException) {
            super(String.format("commission failed and %d exceptions were thrown while unwinding", unwindException.exceptionsThrown.size()));
            this.commissionExceptionThrower = commissionExceptionThrower;
            this.commissionException = commissionException;
            this.unwindException = unwindException;
        }
    }

    static class UnwindException extends RuntimeException {
        public final Map<DependencyLifecycle<?>, RuntimeException> exceptionsThrown;
        public UnwindException(Map<DependencyLifecycle<?>, RuntimeException> exceptionsThrown) {
            super(String.format("%d lifecycle decommission methods threw exception(s): %s", exceptionsThrown.size(), exceptionsThrown.keySet()));
            this.exceptionsThrown = Collections.unmodifiableMap(new LinkedHashMap<>(exceptionsThrown));
        }
    }
}
