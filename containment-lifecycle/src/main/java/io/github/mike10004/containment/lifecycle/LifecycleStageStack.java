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
 * Implementation of a lifecycle that is made up of multiple sequential component lifecycles.
 * To commission a lifecycle stack is to commission each component lifecycle
 * in order, and to decommission a lifecycle is to decommission each component
 * lifecycle in the reverse order.
 *
 * @param <T> type of last commissioned element
 */
public class LifecycleStageStack<T> implements Lifecycle<T> {

    private final List<? extends LifecycleStage<?, ?>> stages;
    private transient final Deque<LifecycleStage<?, ?>> commissioned;

    /**
     * Constructs a new instance.
     * @param preliminaryStages preliminary lifecycles
     * @param finalStage lifecycle that produces the instance that this stack commissions
     */
    private LifecycleStageStack(List<? extends LifecycleStage<?, ?>> stages) {
        this.stages = Collections.unmodifiableList(requireNonNull(stages));
        commissioned = new ArrayDeque<>();
    }

    @VisibleForTesting
    List<? extends LifecycleStage<?, ?>> getStages() {
        return stages;
    }

    /**
     * Creates a new lifecycle stack builder.
     * @return new builder
     */
    public static <T> Builder<T> builder(Lifecycle<T> firstStage) {
        return new Builder<>(LifecycleStage.independent(firstStage));
    }

    /**
     * Creates a new lifecycle stack builder.
     * @return new builder
     */
    public static <T> Builder<T> builder(LifecycleStage<Void, T> firstStage) {
        return new Builder<>(firstStage);
    }

    /**
     * Builder of lifecycle stacks.
     */
    public static class Builder<U> {

        private final Builder<?> parent;
        private final LifecycleStage<?, ?> content;

        protected Builder(Builder<?> parent, LifecycleStage<?, ?> content) {
            this.parent = requireNonNull(parent);
            this.content = requireNonNull(content);
        }

        // root
        protected Builder(LifecycleStage<?, ?> content) {
            this.parent = null;
            this.content = requireNonNull(content);
        }

        /**
         * Builds a lifecycle stack.
         * @param finalStage the final component lifecycle in the stack
         * @param <T> the type of object commissioned by the component lifecycle
         * @return a new lifecycle stack instance
         */
        public LifecycleStageStack<U> build() {
            return new LifecycleStageStack<>(toSequence(new ArrayList<>()));
        }

        /**
         * Adds a component lifecycle.
         * @param stage
         * @return
         */
        public <V> Builder<V> addStage(LifecycleStage<U, V> stage) {
            return new Builder<>(this, stage);
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
        return new StringJoiner(", ", LifecycleStageStack.class.getSimpleName() + "[", "]")
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
            throw new LifecycleStageStackDecommissionException(exceptionsThrown);
        }
    }

    /**
     * Commissions each stage in this stack.
     * If commissioning any lifecycle in the sequence fails, then
     * those already commissioned are decommissioned before throwing
     * the exception that caused the commissioning failure.
     * @return the final commissioned resource
     * @throws LifecycleStageStackCommissionException on error
     */
    @Override
    public T commission() throws LifecycleStageStackCommissionException {
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
            LifecycleStageStackDecommissionException unwindException = null;
            try {
                unwind();
            } catch (LifecycleStageStackDecommissionException e) {
                unwindException = e;
            }
            if (unwindException == null) {
                throw new LifecycleStageStackCommissionException(throwable);
            } else {
                throw new LifecycleStageStackCommissionUnwindException(thrower, throwable, unwindException);
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