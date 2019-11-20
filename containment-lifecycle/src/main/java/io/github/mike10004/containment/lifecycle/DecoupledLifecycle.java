package io.github.mike10004.containment.lifecycle;

/**
 * Lifecycle implementation that employs delegates for commission and decommission.
 * @param <D> lifecycle resource type
 */
public class DecoupledLifecycle<D> implements Lifecycle<D> {

    private transient final Object lock = new Object();
    private final Decommissioner<D> decommissioner;
    private final Commissioner<D> commissioner;
    private volatile D commissioned;

    /**
     * Constructs an instance.
     * @param commissioner commissioner
     * @param decommissioner decommissioner
     */
    public DecoupledLifecycle(Commissioner<D> commissioner, Decommissioner<D> decommissioner) {
        this.decommissioner = decommissioner;
        this.commissioner = commissioner;
    }

    /**
     * Invokes the commissioner.
     * Stores the result for subquent decommissing.
     * @return commissioned resource
     * @throws Exception
     */
    @Override
    public D commission() throws Exception {
        synchronized (lock) {
            commissioned = commissioner.commission();
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
     * @param <R> resource type
     */
    public interface Commissioner<R> {
        /**
         * Commissions the resource.
         * @return resource
         * @throws Exception on error
         */
        R commission() throws Exception;
    }

    /**
     * Interface of a service that decommissions a resource.
     * @param <R> resource type
     */
    public interface Decommissioner<R> {
        /**
         * Decommissions the resource.
         * @param resource resource
         */
        void decommission(R resource);
    }
}
