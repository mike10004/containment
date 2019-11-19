package io.github.mike10004.containment.lifecycle;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Element of a lifestyle stack. The stack is built as a
 * linked list rooted at the first stage.
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
     * Adds a stage, creating a new stacker object..
     * @param stage stage
     * @return a new stacker containing argument stage and all previously-added stage
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
