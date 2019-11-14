package io.github.mike10004.containment.lifecycle;

import static java.util.Objects.requireNonNull;

/**
 * Lifecycle implementation that employs delegates for commission and decommission.
 * @param <D> lifecycle
 */
public class DecoupledLifecycleStage<R, P> implements LifecycleStage<R, P> {

    private transient final Object lock = new Object();
    private final Decommissioner<P> decommissioner;
    private final Commissioner<R, P> commissioner;
    private volatile P commissioned;

    public DecoupledLifecycleStage(Commissioner<R, P> commissioner, Decommissioner<P> decommissioner) {
        this.decommissioner = requireNonNull(decommissioner);
        this.commissioner = requireNonNull(commissioner);
    }

    /**
     * Invokes the commissioner.
     * Stores the result for subquent decommissing.
     * @return commissioned resource
     * @throws Exception
     */
    @Override
    public P commission(R requirement) throws Exception {
        synchronized (lock) {
            commissioned = commissioner.commission(requirement);
            return commissioned;
        }
    }

    /**
     * Invokes the decommissioner if and only if commission has succeeded.
     */
    @Override
    public void decommission() {
        synchronized (lock) {
            if (commissioned != null) {
                decommissioner.decommission(commissioned);
                commissioned = null;
            }
        }
    }

    /**
     * Interface of a service that commissions a resource.
     */
    public interface Commissioner<R, P> {

        /**
         * Commissions the resource.
         * @param requirement requirement
         * @return resource
         * @throws Exception on error
         */
        P commission(R requirement) throws Exception;
    }

    /**
     * Interface of a service that decommissions a resource.
     * @param <P> resource type
     */
    public interface Decommissioner<P> {
        /**
         * Decommissions the resource.
         * @param resource resource
         */
        void decommission(P resource);
    }
}
