package io.github.mike10004.containment.lifecycle;

public class DecoupledLifecycle<D> implements Lifecycle<D> {

    private transient final Object lock = new Object();
    private final Decommissioner<D> decommissioner;
    private final Commissioner<D> commissioner;
    private volatile D commissioned;

    public DecoupledLifecycle(Commissioner<D> commissioner, Decommissioner<D> decommissioner) {
        this.decommissioner = decommissioner;
        this.commissioner = commissioner;
    }

    @Override
    public D commission() throws Exception {
        synchronized (lock) {
            commissioned = commissioner.commission();
            return commissioned;
        }
    }

    @Override
    public void decommission() {
        synchronized (lock) {
            if (commissioned != null) {
                decommissioner.decommission(commissioned);
                commissioned = null;
            }
        }
    }

    public interface Commissioner<D> {
        D commission() throws Exception;
    }

    public interface Decommissioner<D> {
        void decommission(D value);
    }
}
