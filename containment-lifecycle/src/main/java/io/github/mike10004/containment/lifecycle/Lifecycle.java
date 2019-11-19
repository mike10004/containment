package io.github.mike10004.containment.lifecycle;

/**
 * Interface of a service that provides methods to execute the full lifecycle of a resource.
 * A lifecycle includes the commission and decommission actions.
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

}
