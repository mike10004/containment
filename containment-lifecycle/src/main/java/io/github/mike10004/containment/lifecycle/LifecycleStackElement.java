package io.github.mike10004.containment.lifecycle;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Element of a lifestyle stack. A stack element has a corresponding lifecycle
 * stage and a link to stack element corresponding to the previous stage in the
 * lifecycle. The first lifecycle stack element is the root and has no previous stage.
 * The root stack element is created by {@link LifecycleStack#startingAt(Lifecycle)}.
 * Add stages with {@link #andThen(LifecycleStage)}.
 * When all stages have been added, build the multi-stage lifecycle
 * with {@link #toSequence()}.
 * @param <U> type of resource produced by the stage corresponding to this element
 */
public class LifecycleStackElement<U> {

    private final LifecycleStackElement<?> parent;
    private final LifecycleStage<?, U> stage;

    private <T> LifecycleStackElement(LifecycleStackElement<T> parent, LifecycleStage<T, U> stage) {
        this.parent = parent;
        this.stage = requireNonNull(stage);
    }

    static <T> LifecycleStackElement<T> root(LifecycleStage<?, T> content) {
        return new LifecycleStackElement<>(null, content);
    }

    /**
     * Builds a lifecycle that is the sequence of stages starting at the root and finishing with this element's stage.
     * @return a new lifecycle instance
     */
    public Lifecycle<U> toSequence() {
        return new LifecycleStack<>(toSequence(new ArrayList<>()));
    }

    /**
     * Adds a stage, creating a new stack element.
     * @param stage stage
     * @param <V> type of resource produced by the next stage
     * @return a new stack element containing argument stage and all previously-added stage
     */
    public <V> LifecycleStackElement<V> andThen(LifecycleStage<U, V> stage) {
        return new LifecycleStackElement<>(this, stage);
    }

    private List<LifecycleStage<?, ?>> toSequence(List<LifecycleStage<?, ?>> list) {
        if (parent != null) {
            parent.toSequence(list);
        }
        list.add(stage);
        return list;
    }
}
