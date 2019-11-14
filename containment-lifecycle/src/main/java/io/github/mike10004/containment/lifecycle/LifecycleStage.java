package io.github.mike10004.containment.lifecycle;

public interface LifecycleStage<R, P> {
    P commission(R requirement) throws Exception;
    void decommission();

    static <T> LifecycleStage<Void, T> independent(Lifecycle<T> lifecycle) {
        return new LifecycleStage<Void, T>() {
            @Override
            public T commission(Void requirement) throws Exception {
                return lifecycle.commission();
            }

            @Override
            public void decommission() {
                lifecycle.decommission();
            }
        };
    }
}
