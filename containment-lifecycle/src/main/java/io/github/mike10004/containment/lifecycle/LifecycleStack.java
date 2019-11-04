package io.github.mike10004.containment.lifecycle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 *
 * @param <T> type of top element of the stack (last created, first destroyed)
 */
public class LifecycleStack<T> implements DependencyLifecycle<T> {

    private final List<? extends DependencyLifecycle<?>> others;
    private final Deque<DependencyLifecycle<?>> commissioned;
    private final DependencyLifecycle<T> top;

    public LifecycleStack(List<? extends DependencyLifecycle<?>> others, DependencyLifecycle<T> top) {
        commissioned = new ArrayDeque<>();
        this.top = top;
        this.others = new ArrayList<>(others);
    }

    private void unwind() {
        List<RuntimeException> exceptionsThrown = new ArrayList<>();
        while (!commissioned.isEmpty()) {
            DependencyLifecycle<?> lifecycle = commissioned.pop();
            try {
                lifecycle.decommission();
            } catch (RuntimeException e) {
                exceptionsThrown.add(e);
            }
        }
        if (!exceptionsThrown.isEmpty()) {
            throw new UnwindException(exceptionsThrown);
        }
    }

    @Override
    public T commission() throws Exception {
        Exception throwable = null;
        for (DependencyLifecycle<?> lifecycle : others) {
            try {
                lifecycle.commission();
                commissioned.push(lifecycle);
            } catch (Exception e) {
                throwable = e;
            }
        }
        if (throwable == null) {
            try {
                T top = this.top.commission();
                commissioned.push(this.top);
                return top;
            } catch (Exception e) {
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
            throw new CommissionFailedAndUnwindFailedException(throwable, unwindException);
        }
    }

    @Override
    public void decommission() {
        unwind();
    }

    static class CommissionFailedAndUnwindFailedException extends Exception {
        public final Exception commissionException;
        public final UnwindException unwindException;

        CommissionFailedAndUnwindFailedException(Exception commissionException, UnwindException unwindException) {
            super(String.format("commission failed and %d exceptions were thrown while unwinding", unwindException.exceptionsThrown.size()));
            this.commissionException = commissionException;
            this.unwindException = unwindException;
        }
    }

    static class UnwindException extends RuntimeException {
        public final List<RuntimeException> exceptionsThrown;
        public UnwindException(List<RuntimeException> exceptionsThrown) {
            super(String.format("%d lifecycle decommission methods threw exception(s)", exceptionsThrown.size()));
            this.exceptionsThrown = Collections.unmodifiableList(new ArrayList<>(exceptionsThrown));
        }
    }
}
