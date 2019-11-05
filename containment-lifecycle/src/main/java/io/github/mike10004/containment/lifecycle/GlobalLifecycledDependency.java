package io.github.mike10004.containment.lifecycle;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class GlobalLifecycledDependency<D> extends LifecycledDependency<D> {

    private final Consumer<? super Thread> addShutdownHookMethod;

    /**
     * Constructs an instance of the rule.
     * @param innerRule
     */
    public GlobalLifecycledDependency(DependencyLifecycle<D> innerRule) {
        this(innerRule, ignore -> {});
    }

    public GlobalLifecycledDependency(DependencyLifecycle<D> innerRule, Consumer<? super LifecycleEvent> eventListener) {
        this(innerRule, eventListener, Runtime.getRuntime()::addShutdownHook);
    }

    public GlobalLifecycledDependency(DependencyLifecycle<D> innerRule, Consumer<? super LifecycleEvent> eventListener, Consumer<? super Thread> addShutdownHookMethod) {
        super(innerRule, eventListener);
        this.addShutdownHookMethod = requireNonNull(addShutdownHookMethod);
    }

    protected Computation<D> computeOnce() {
        try {
            D val = doCommission();
            Thread thread = new Thread(this::finishLifecycleNow);
            addRuntimeShutdownHook(thread);
            return GlobalComputation.succeeded(val, thread);
        } catch (Exception t) {
            return GlobalComputation.failed(t);
        }
    }

    private void addRuntimeShutdownHook(Thread thread) {
        notify(LifecycleEvent.Category.NOTICE, "DependencyManager.addRuntimeShutdownHook() entered");
        addShutdownHookMethod.accept(thread);
    }

    @Override
    public final void finishLifecycle() {
        notify(LifecycleEvent.Category.NOTICE, "skipping finishLifecycle because a runtime shutdown hook will handle that");
    }

    public final void finishLifecycleNow() {
        super.finishLifecycle();
    }

    /**
     * Reports a teardown error to the process standard error stream.
     * It does us no good to bubble up an exception in a shutdown hook,
     * so we merely report the error.
     *
     * // TODO control reporting with instance flags, system properties, and environment variables
     * @param t
     */
    @Override
    protected void handleTearDownError(RuntimeException t) {
        t.printStackTrace(System.err);
    }

    private static class GlobalComputation<D> extends Computation<D> {

        private final String stringification;

        private GlobalComputation(D provisioned, Thread thread, Throwable exception) {
            super(provisioned, exception);
            stringification = String.format("BeforeInvocation{value=%s,tearDown=%s,exception=%s}", provisioned, thread, exception);
        }

        public static <D> Computation<D> succeeded(D provisioned, Thread thread) {
            return new GlobalComputation<>(provisioned, thread, null);
        }

        public static <D> Computation<D> failed(Throwable t) {
            return new GlobalComputation<>(null, null, t);
        }

        @Override
        public String toString() {
            return stringification;
        }

    }
}
