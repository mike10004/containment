package io.github.mike10004.containment.lifecycle;

/**
 * Builder of simple lifecycle stacks. A simple lifecycle stack
 * is a sequence of independent lifecycles.
 * This acts more like a traditional builder than the
 * {@link io.github.mike10004.containment.lifecycle.ProgressiveLifecycleStack.Stacker}
 * instances do, in that the {@link #addStage(Lifecycle)} method
 * returns the same builder instance. The difference between
 * lifecycle stacks built by this builder and those built with
 * stackers is that the component lifecycles here cannot depend
 * on resources commissioned by previous component lifecycles
 * in the sequence.
 */
public class SimpleLifecycleStackBuilder {

    private ProgressiveLifecycleStack.Stacker stacker;

    protected SimpleLifecycleStackBuilder() {
    }

    /**
     * Creates a new lifecycle stack builder.
     * @return a new builder instance
     */
    public static SimpleLifecycleStackBuilder create() {
        return new SimpleLifecycleStackBuilder();
    }

    /**
     * Builds a lifecycle stack.
     * @param finalStage the final component lifecycle in the stack
     * @param <T> the type of object commissioned by the component lifecycle
     * @return a new lifecycle stack instance
     */
    public synchronized <T> Lifecycle<T> finish(Lifecycle<T> finalStage) {
        if (stacker == null) {
            return finalStage;
        } else {
            return append(finalStage).toSequence();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ProgressiveLifecycleStack.Stacker<T> append(Lifecycle<T> stage) {
        return stacker = stacker.andThen(LifecycleStage.requirementless(stage));
    }

    /**
     * Adds a component lifecycle.
     * @param stage stage
     * @return this builder
     */
    public synchronized SimpleLifecycleStackBuilder addStage(Lifecycle<?> stage) {
        if (stacker == null) {
            stacker = ProgressiveLifecycleStack.startingAt(stage);
        } else {
            stacker = append(stage);
        }
        return this;
    }
}
