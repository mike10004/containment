package io.github.mike10004.containment.lifecycle;

/**
 * Interface of a service that defines one stage of a lifecycle stack.
 * @param <R> required resource type
 * @param <P> produced resource type
 */
public interface LifecycleStage<R, P> {

    /**
     * Produces a resource.
     * @param requirement the required resource
     * @return the produced resource
     * @throws Exception
     */
    P commission(R requirement) throws Exception;

    /**
     * Decommissions the produced resource.
     */
    void decommission();

}
