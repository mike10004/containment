package io.github.mike10004.containment.lifecycle;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class AutoCloseableDecommissioner<T extends AutoCloseable> implements DecoupledLifecycle.Decommissioner<T>, DecoupledLifecycleStage.Decommissioner<T> {

    public static final ExceptionReaction DEFAULT_EXCEPTION_REACTION = ExceptionReaction.WRAP_WITH_RUNNABLE;

    private final ExceptionReaction exceptionReaction;

    public AutoCloseableDecommissioner() {
        this(DEFAULT_EXCEPTION_REACTION);
    }

    public AutoCloseableDecommissioner(ExceptionReaction exceptionReaction) {
        this.exceptionReaction = requireNonNull(exceptionReaction);
    }

    @Override
    public void decommission(T resource) {
        try {
            resource.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            exceptionReaction.react(e);
        }
    }

    public static <T> DecoupledLifecycleStage.Decommissioner<T> byTransform(Function<? super T, AutoCloseable> transform) {
        return byTransform(transform, DEFAULT_EXCEPTION_REACTION);
    }

    public static <T> DecoupledLifecycleStage.Decommissioner<T> byTransform(Function<? super T, AutoCloseable> transform, ExceptionReaction exceptionReaction) {
        return new DecoupledLifecycleStage.Decommissioner<T>() {

            private final AutoCloseableDecommissioner<AutoCloseable> delegate = new AutoCloseableDecommissioner<>(exceptionReaction);

            @Override
            public void decommission(T resource) {
                delegate.decommission(transform.apply(resource));
            }
        };
    }
}
