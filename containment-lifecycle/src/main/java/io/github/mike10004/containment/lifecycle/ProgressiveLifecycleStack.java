package io.github.mike10004.containment.lifecycle;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
 * @see ProgressiveContainerLifecycles
 */
public class ProgressiveLifecycleStack<T> implements Lifecycle<T> {

    private final List<? extends LifecycleStage<?, ?>> stages;
    private transient final Deque<LifecycleStage<?, ?>> commissioned;

    private ProgressiveLifecycleStack(List<? extends LifecycleStage<?, ?>> stages) {
        this.stages = Collections.unmodifiableList(requireNonNull(stages));
        commissioned = new ArrayDeque<>();
    }

    @VisibleForTesting
    List<? extends LifecycleStage<?, ?>> getStages() {
        return stages;
    }

    /**
     * Creates a new lifecycle stacker.
     * @return new builder
     */
    public static <T> Stacker<T> startingAt(Lifecycle<T> firstStage) {
        return new Stacker<>(LifecycleStage.independent(firstStage));
    }

    /**
     * Creates a new lifecycle stack builder.
     * @return new builder
     */
    public static <T> Stacker<T> startingAt(LifecycleStage<Void, T> firstStage) {
        return new Stacker<>(firstStage);
    }

    /**
     * Progressive lifecycle stage stacker. A stacker instance is an element of a
     * linked list rooted at the first stage.
     */
    public static class Stacker<U> {

        private final Stacker<?> parent;
        private final LifecycleStage<?, ?> content;

        protected Stacker(Stacker<?> parent, LifecycleStage<?, ?> content) {
            this.parent = requireNonNull(parent);
            this.content = requireNonNull(content);
        }

        // root
        protected Stacker(LifecycleStage<?, ?> content) {
            this.parent = null;
            this.content = requireNonNull(content);
        }

        /**
         * Builds a progressive lifecycle stack.
         * @return a new progressive lifecycle stack instance
         */
        public ProgressiveLifecycleStack<U> build() {
            return new ProgressiveLifecycleStack<>(toSequence(new ArrayList<>()));
        }

        /**
         * Adds a stage, creating a new stacker object..
         * @param stage stage
         * @return a new stacker containing argument stage and all previously-added stage
         */
        public <V> Stacker<V> andThen(LifecycleStage<U, V> stage) {
            return new Stacker<>(this, stage);
        }

        private List<LifecycleStage<?, ?>> toSequence(List<LifecycleStage<?, ?>> list) {
            if (parent != null) {
                parent.toSequence(list);
            }
            list.add(content);
            return list;
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProgressiveLifecycleStack.class.getSimpleName() + "[", "]")
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
            throw new ProgressiveLifecycleStackDecommissionException(exceptionsThrown);
        }
    }

    /**
     * Commissions each stage in this stack.
     * If commissioning any lifecycle in the sequence fails, then
     * those already commissioned are decommissioned before throwing
     * the exception that caused the commissioning failure.
     * @return the final commissioned resource
     * @throws ProgressiveLifestyleStackCommissionException on error
     */
    @Override
    public T commission() throws ProgressiveLifestyleStackCommissionException {
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
            ProgressiveLifecycleStackDecommissionException unwindException = null;
            try {
                unwind();
            } catch (ProgressiveLifecycleStackDecommissionException e) {
                unwindException = e;
            }
            if (unwindException == null) {
                throw new ProgressiveLifestyleStackCommissionException(throwable);
            } else {
                throw new ProgressiveLifestyleStackCommissionUnwindException(thrower, throwable, unwindException);
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
