package io.github.mike10004.containment.lifecycle;

import javax.annotation.Nullable;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a provider of a resource that has a lifecycle.
 * @param <D>
 */
public class LifecyclingCachingProvider<D> implements CachingProvider<D> {

    private final Lifecycle<D> lifecycle;
    private final ConcurrentCache concurrentCache;
    private final Consumer<? super LifecycleEvent> eventListener;
    private final AtomicBoolean finishInvoked;

    /**
     * Constructs an instance.
     * @param lifecycle the lifecycle
     */
    public LifecyclingCachingProvider(Lifecycle<D> lifecycle) {
        this(lifecycle, LifecycleEvent.inactiveConsumer());
    }

    /**
     * Constructs an instance.
     * @param lifecycle lifecycle
     * @param eventListener event listener
     */
    public LifecyclingCachingProvider(Lifecycle<D> lifecycle, Consumer<? super LifecycleEvent> eventListener) {
        this.lifecycle = requireNonNull(lifecycle);
        concurrentCache = new ConcurrentCache();
        this.eventListener = requireNonNull(eventListener);
        finishInvoked = new AtomicBoolean(false);
    }

    private static class LifecycleFinishedException extends RuntimeException {}

    /**
     * Returns a provision, after computing or recalling the cached computation result.
     * @return the provision
     */
    @Override
    public final Provision<D> provide() {
        boolean alreadyInvoked = finishInvoked.get();
        if (alreadyInvoked) {
            return Provision.failed(new LifecycleFinishedException());
        }
        notify(LifecycleEvent.Category.PROVIDE_STARTED);
        AtomicBoolean computed = new AtomicBoolean(false);
        Provision<D> invocation = concurrentCache.compute(new Supplier<Provision<D>>(){
            @Override
            public Computation<D> get() {
                computed.set(true);
                return computeOnce();
            }
        });
        notify(LifecycleEvent.Category.PROVIDE_COMPLETED, String.format("%s %s", computed.get() ? "computed" : "recalled", invocation));
        return invocation;
    }

    protected D doCommission() throws Exception {
        notify(LifecycleEvent.Category.COMMISSION_STARTED);
        D commissioned = lifecycle.commission();
        Verify.verifyNotNull(commissioned, "lifecycle produced non-null commission() result");
        return commissioned;
    }

    protected Computation<D> computeOnce() {
        try {
            D val = doCommission();
            notify(LifecycleEvent.Category.COMMISSION_SUCCEEDED);
            return Computation.succeeded(val);
        } catch (Throwable t) {
            notify(LifecycleEvent.Category.COMMISSION_FAILED);
            return Computation.failed(t);
        }
    }

    /**
     * Finishes the lifecycle of the cached object.
     */
    public void finishLifecycle() {
        boolean firstInvocation = finishInvoked.compareAndSet(false, true);
        if (!firstInvocation) {
            return;
        }
        notify(LifecycleEvent.of(LifecycleEvent.Category.FINISH_STARTED));
        try {
            lifecycle.decommission();
        } catch (RuntimeException t) {
            handleTearDownError(t);
        }
        notify(LifecycleEvent.of(LifecycleEvent.Category.FINISH_COMPLETED));
    }

    protected void handleTearDownError(RuntimeException t) {
        throw t;
    }

    private class ConcurrentCache {

        private final ConcurrentMap<Object, Provision<D>> concurrencyManager = new ConcurrentHashMap<>(1);
        private transient final Object computeKey = new Object();

        public Provision<D> compute(Supplier<Provision<D>> computer) {
            return concurrencyManager.computeIfAbsent(computeKey, k -> computer.get());
        }

        @Override
        public String toString() {
            return String.format("ConcurrentCache[size=%d]", concurrencyManager.size());
        }

        @Nullable
        public Provision<D> getIfPresent() {
            return concurrencyManager.get(computeKey);
        }
    }

    @Nullable
    protected Provision<D> getProvisionIfAvailable() {
        @Nullable Provision<D> provision = concurrentCache.getIfPresent();
        return provision;
    }

    protected void notify(LifecycleEvent.Category category, String message) {
        notify(new LifecycleEvent(category, message));
    }

    protected void notify(LifecycleEvent event) {
        eventListener.accept(event);
    }

    protected void notify(LifecycleEvent.Category category) {
        eventListener.accept(LifecycleEvent.of(category));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LifecyclingCachingProvider.class.getSimpleName() + "[", "]")
                .add("lifecycle=" + lifecycle)
                .add("concurrentCache=" + concurrentCache)
                .add("eventListener=" + eventListener)
                .toString();
    }
}
