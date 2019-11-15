package io.github.mike10004.containment.lifecycle;

public interface LifecycleStage<R, P> {

    P commission(R requirement) throws Exception;

    void decommission();

    static <T> LifecycleStage<Void, T> independent(Lifecycle<T> lifecycle) {
        return lifecycle.asStage();
    }

    static <T> LifecycleStage<Object, T> requirementless(Lifecycle<T> lifecycle) {
        return lifecycle.asStage();
    }

    default boolean isEquivalent(Object other) {
        return equals(other);
    }
}
