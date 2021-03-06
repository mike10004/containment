package io.github.mike10004.containment.lifecycle;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a lifecycle that is made up of multiple stages,
 * where each stage is provided the resource commissioned by the previous
 * stage.
 * To commission a progressive lifecycle stack is to commission each stage
 * in order, and to decommission a lifecycle is to decommission each stage
 * in reverse order.
 *
 * @param <T> type of last commissioned element
 * @see ContainerLifecycles
 */
public class LifecycleStack<T> implements Lifecycle<T> {

    private final List<? extends LifecycleStage<?, ?>> stages;
    private transient final Deque<LifecycleStage<?, ?>> commissioned;

    LifecycleStack(List<? extends LifecycleStage<?, ?>> stages) {
        this.stages = Collections.unmodifiableList(requireNonNull(stages));
        commissioned = new ArrayDeque<>();
    }

    @VisibleForTesting
    List<? extends LifecycleStage<?, ?>> getStages() {
        return stages;
    }

    /**
     * Creates a new lifecycle stack root element.
     * @param firstStage first stage of the lifecycle
     * @param <T> type of resource produced by the first stage
     * @return a new stack root element
     */
    public static <T> LifecycleStackElement<T> startingAt(Lifecycle<T> firstStage) {
        return startingAt(new RequirementlessLifecycleStage<>(firstStage));
    }

    @VisibleForTesting
    static <T> LifecycleStackElement<T> startingAt(LifecycleStage<Void, T> firstStage) {
        return  LifecycleStackElement.root(firstStage);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LifecycleStack.class.getSimpleName() + "[", "]")
                .add("stages=" + stages)
                .add("commissioned.size=" + commissioned.size())
                .toString();
    }

    private void unwind() throws LifecycleStackDecommissionException {
        Map<LifecycleStage<?, ?>, RuntimeException> exceptionsThrown = new LinkedHashMap<>();
        while (!commissioned.isEmpty()) {
            LifecycleStage<?, ?> lifecycle = commissioned.pop();
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
     * Commissions each stage in this stack.
     * If commissioning any lifecycle in the sequence fails, then
     * those already commissioned are decommissioned before throwing
     * the exception that caused the commissioning failure.
     * @return the final commissioned resource
     * @throws LifecycleStackCommissionException on error
     */
    @Override
    public T commission() throws LifecycleStackCommissionException {
        LifecycleStage<?, ?> thrower = null;
        Exception throwable = null;
        Object lastCommissioned = null;
        for (LifecycleStage<?, ?> stage : stages) {
            try {
                // We can trust the cast here because it is enforced at compile-time by the Builder class.
                //noinspection unchecked
                lastCommissioned = ((LifecycleStage)stage).commission(lastCommissioned);
            } catch (Exception e) {
                throwable = e;
                thrower = stage;
                break;
            }
            commissioned.push(stage);
        }
        if (commissioned.size() == stages.size()) {
            // Same as above; cast is trustworthy because of Builder
            //noinspection unchecked
            return (T) lastCommissioned;
        } else {
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
