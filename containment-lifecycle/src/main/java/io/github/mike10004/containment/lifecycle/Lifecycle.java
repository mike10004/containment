package io.github.mike10004.containment.lifecycle;

/**
 * Interface of a service that provides methods to execute the full lifecycle of a resource.
 * A lifecycle includes the commission and decommission stages.
 * @param <D> provided resource type
 */
public interface Lifecycle<D> {

    /**
     * Commissions the resource.
     * @return the resource
     * @throws Exception on error
     */
    D commission() throws Exception;

    /**
     * Decommissions the resource.
     */
    void decommission();

    default <R> LifecycleStage<R, D> asStage() {
        Lifecycle<D> self = this;
        return new LifecycleStage<R, D>() {
            @Override
            public D commission(R requirement) throws Exception {
                return self.commission();
            }

            @Override
            public void decommission() {
                self.decommission();
            }

            @Override
            public boolean isEquivalent(Object other) {
                return super.equals(other) || self.equals(other);
            }

            @Override
            public String toString() {
                return String.format("LifecycleStage{%s}", self);
            }
        };
    }
}
