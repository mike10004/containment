package io.github.mike10004.containment.lifecycle;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 *
 * @param <T> type of top element of the stack (last created, first destroyed)
 */
public class LifecycleStack<T> implements Lifecycle<T> {

    private final Iterable<? extends Lifecycle<?>> others;
    private final Deque<Lifecycle<?>> commissioned;
    private transient final Lifecycle<T> top;

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

    private void unwind() {
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
            throw new UnwindException(exceptionsThrown);
        }
    }

    @Override
    public T commission() throws Exception {
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
        public final Lifecycle<?> commissionExceptionThrower;
        public final Exception commissionException;
        public final UnwindException unwindException;

        public CommissionFailedAndUnwindFailedException(Lifecycle<?> commissionExceptionThrower, Exception commissionException, UnwindException unwindException) {
            super(String.format("commission failed and %d exceptions were thrown while unwinding", unwindException.exceptionsThrown.size()));
            this.commissionExceptionThrower = commissionExceptionThrower;
            this.commissionException = commissionException;
            this.unwindException = unwindException;
        }
    }

    static class UnwindException extends RuntimeException {
        public final Map<Lifecycle<?>, RuntimeException> exceptionsThrown;
        public UnwindException(Map<Lifecycle<?>, RuntimeException> exceptionsThrown) {
            super(String.format("%d lifecycle decommission methods threw exception(s): %s", exceptionsThrown.size(), exceptionsThrown.keySet()));
            this.exceptionsThrown = Collections.unmodifiableMap(new LinkedHashMap<>(exceptionsThrown));
        }
    }
}
