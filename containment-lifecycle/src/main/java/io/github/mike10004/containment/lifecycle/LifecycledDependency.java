package io.github.mike10004.containment.lifecycle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class LifecycledDependency<D> implements LazyDependency<D> {

    private final DependencyLifecycle<D> lifecycle;
    private final ExecutionManager executionManager;
    private final Consumer<? super LifecycleEvent> eventListener;

    /**
     * Constructs an instance of the rule.
     * @param lifecycle
     */
    public LifecycledDependency(DependencyLifecycle<D> lifecycle) {
        this(lifecycle, ignore -> {});
    }

    public LifecycledDependency(DependencyLifecycle<D> lifecycle, Consumer<? super LifecycleEvent> eventListener) {
        this.lifecycle = requireNonNull(lifecycle);
        executionManager = new ExecutionManager();
        this.eventListener = requireNonNull(eventListener);
    }

    /**
     * Invokes this instance's rule delegate setup method exactly once.
     * The setup method is invoked only the first time this method is invoked,
     * and on subsequent invocations of this method,
     *
     * @throws FirstProvisionFailedException
     */
    @Override
    public final Provision<D> provide() {
        notify(LifecycleEvent.Category.PROVIDE_STARTED);
        AtomicBoolean computed = new AtomicBoolean(false);
        Provision<D> invocation = executionManager.compute(new Supplier<Provision<D>>(){
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

    public void finishLifecycle() {
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

    private class ExecutionManager {

        private final ConcurrentMap<Object, Provision<D>> handler = new ConcurrentHashMap<>(1);
        private final Object setupKey = new Object();

        public Provision<D> compute(Supplier<Provision<D>> computer) {
            return handler.computeIfAbsent(setupKey, k -> computer.get());
        }
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

}
