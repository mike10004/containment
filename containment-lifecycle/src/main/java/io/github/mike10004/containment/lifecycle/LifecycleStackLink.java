package io.github.mike10004.containment.lifecycle;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Element of a lifestyle stack. The stack is built as a
 * linked list rooted at the first stage.
 */
public class LifecycleStackLink<U> {

    private final LifecycleStackLink<?> parent;
    private final LifecycleStage<?, U> content;

    private <T> LifecycleStackLink(LifecycleStackLink<T> parent, LifecycleStage<T, U> content) {
        this.parent = parent;
        this.content = requireNonNull(content);
    }

    static <T> LifecycleStackLink<T> root(LifecycleStage<?, T> content) {
        return new LifecycleStackLink<>(null, content);
    }

    /**
     * Builds a progressive lifecycle stack.
     * @return a new progressive lifecycle stack instance
     */
    public Lifecycle<U> toSequence() {
        return new LifecycleStack<>(toSequence(new ArrayList<>()));
    }

    /**
     * Adds a stage, creating a new stacker object..
     * @param stage stage
     * @return a new stacker containing argument stage and all previously-added stage
     */
    public <V> LifecycleStackLink<V> andThen(LifecycleStage<U, V> stage) {
        return new LifecycleStackLink<>(this, stage);
    }

    private List<LifecycleStage<?, ?>> toSequence(List<LifecycleStage<?, ?>> list) {
        if (parent != null) {
            parent.toSequence(list);
        }
        list.add(content);
        return list;
    }
}
