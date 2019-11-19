package io.github.mike10004.containment.lifecycle;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a provider of a resource whose lifecycle begins on first
 * retrieval and ends on JVM termination.
 * @param <D> resource type
 */
final class GlobalLifecyclingCachingProvider<D> extends LifecyclingCachingProvider<D> {

    private final RuntimeShutdownHookManager addShutdownHookMethod;

    /**
     * Constructs an instance.
     * @param lifecycle lifecycle
     */
    public GlobalLifecyclingCachingProvider(Lifecycle<D> lifecycle) {
        this(lifecycle, ignore -> {});
    }

    /**
     * Constructs an instance.
     * @param lifecycle lifecycle
     * @param eventListener event listener
     */
    public GlobalLifecyclingCachingProvider(Lifecycle<D> lifecycle, Consumer<? super LifecycleEvent> eventListener) {
        this(lifecycle, eventListener, RuntimeShutdownHookManager.managing(Runtime.getRuntime()));
    }

    @VisibleForTesting
    GlobalLifecyclingCachingProvider(Lifecycle<D> innerRule, Consumer<? super LifecycleEvent> eventListener, RuntimeShutdownHookManager addShutdownHookMethod) {
        super(innerRule, eventListener);
        this.addShutdownHookMethod = requireNonNull(addShutdownHookMethod);
    }

    interface RuntimeShutdownHookManager {
        void add(Thread t);
        @SuppressWarnings("UnusedReturnValue")
        boolean remove(Thread t);
        static RuntimeShutdownHookManager managing(Runtime rt) {
            return new RuntimeShutdownHookManager() {
                @Override
                public void add(Thread t) {
                    rt.addShutdownHook(t);
                }

                @Override
                public boolean remove(Thread t) {
                    return rt.removeShutdownHook(t);
                }
            };
        }
    }

    @Override
    protected Provision<D> computeOnce() {
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
        notify(LifecycleEvent.Category.NOTICE, "addRuntimeShutdownHook() entered");
        addShutdownHookMethod.add(thread);
    }

    @Override
    public final void finishLifecycle() {
        notify(LifecycleEvent.Category.NOTICE, "skipping finishLifecycle because a runtime shutdown hook will handle that");
    }

    private void finishLifecycleNow() {
        super.finishLifecycle();
    }

    /**
     * Finishes this instance's lifecycle now, instead of letting it finish on JVM shutdown.
     */
    public final void finishLifecycleNowInsteadOfOnShutdown() {
        finishLifecycleNow();
        removeShutdownHook();
    }

    private void removeShutdownHook() {
        @Nullable Provision<D> provision = getProvisionIfAvailable();
        @Nullable Thread shutdownHook = null;
        if (provision != null) {
            shutdownHook = ((GlobalComputation<D>)provision).shutdownHook;
        }
        if (shutdownHook != null) {
            addShutdownHookMethod.remove(shutdownHook);
        }
    }

    /**
     * Reports a teardown error to the process standard error stream.
     * It does us no good to bubble up an exception in a shutdown hook,
     * so we merely report the error.
     *
     * // TODO control reporting with instance flags, system properties, and environment variables
     * @param t the error
     */
    @Override
    protected void handleTearDownError(RuntimeException t) {
        t.printStackTrace(System.err);
    }

    private static class GlobalComputation<D> extends Computation<D> {

        public final Thread shutdownHook;
        private final String stringification;

        private GlobalComputation(D provisioned, Thread thread, Throwable exception) {
            super(provisioned, exception);
            this.shutdownHook = thread;
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
