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
    private final Consumer<? super String> eventListener;

    /**
     * Constructs an instance of the rule.
     * @param lifecycle
     */
    public LifecycledDependency(DependencyLifecycle<D> lifecycle) {
        this(lifecycle, ignore -> {});
    }

    public LifecycledDependency(DependencyLifecycle<D> lifecycle, Consumer<? super String> eventListener) {
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
        notify("LifecycledDependency.provide() entered");
        AtomicBoolean computed = new AtomicBoolean(false);
        Provision<D> invocation = executionManager.compute(new Supplier<Provision<D>>(){
            @Override
            public Computation<D> get() {
                computed.set(true);
                return computeOnce();
            }
        });
        notify(String.format("LifecycledDependency.provide() %s %s", computed.get() ? "computed" : "got", invocation));
        return invocation;
    }

    protected final D doCommission() throws Exception {
        D commissioned = lifecycle.commission();
        Verify.verifyNotNull(commissioned, "lifecycle produced non-null provision() result");
        return commissioned;
    }

    protected Computation<D> computeOnce() {
        try {
            D val = doCommission();
            return Computation.succeeded(val);
        } catch (Throwable t) {
            return Computation.failed(t);
        }
    }

    public void finishLifecycle() {
        notify("LifecycledDependency.finishLifecycle() entered");
        try {
            lifecycle.decommission();
        } catch (RuntimeException t) {
            handleTearDownError(t);
        }
        notify("LifecycledDependency.finishLifecycle() exiting");
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

    protected void notify(String message) {
        eventListener.accept(message);
    }

}
