package io.github.mike10004.containment.lifecycle;

import java.util.function.Consumer;

public final class GlobalLifecycledDependency<D> extends LifecycledDependency<D> {

    /**
     * Constructs an instance of the rule.
     * @param innerRule
     */
    public GlobalLifecycledDependency(DependencyLifecycle<D> innerRule) {
        this(innerRule, ignore -> {});
    }

    public GlobalLifecycledDependency(DependencyLifecycle<D> innerRule, Consumer<? super String> eventListener) {
        super(innerRule, eventListener);
    }

    protected Computation<D> computeOnce() {
        try {
            D val = doCommission();
            Thread thread = new Thread(this::finishLifecycle);
            addRuntimeShutdownHook(thread);
            return GlobalComputation.succeeded(val, thread);
        } catch (Exception t) {
            return GlobalComputation.failed(t);
        }
    }

    protected void addRuntimeShutdownHook(Thread thread) {
        notify("DependencyManager.addRuntimeShutdownHook() entered");
        Runtime.getRuntime().addShutdownHook(thread);
    }

    @Override
    protected void handleTearDownError(RuntimeException t) {
        // it does us no good to bubble up an exception in a shutdown hook, so we merely report the error
        t.printStackTrace(System.err); // TODO control reporting with instance flags, system properties, and environment variables
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
