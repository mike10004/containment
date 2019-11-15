package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Class that contains utility methods for building progressive container lifecycles.
 */
public class ProgressiveContainerLifecycles {

    private ProgressiveContainerLifecycles() {}

    private static class ContainerCreatorStage extends DecoupledLifecycle<ContainerCreator> {
        public ContainerCreatorStage(Commissioner<ContainerCreator> commissioner) {
            super(commissioner, new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("ContainerCreatorStage@%08x", hashCode());
        }
    }

    private static class StartableContainerStage extends DecoupledLifecycleStage<ContainerCreator, StartableContainer> {

        public StartableContainerStage(ContainerParametry containerParametry) {
            super(creator -> creator.create(containerParametry), new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("StartableContainerStage@%08x", hashCode());
        }
    }

    private static class SimpleStartedContainerStage extends DecoupledLifecycleStage<StartableContainer, StartedContainer> {

        public SimpleStartedContainerStage() {
            super(StartableContainer::start, new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("SimpleStartedContainerStage@%08x", hashCode());
        }
    }

    private static class ActionStageResult<C, T> {

        public final C container;
        public final T content;

        protected ActionStageResult(C container, T content) {
            this.container = requireNonNull(container);
            this.content = content;
        }

    }

    private static final class PreStartResult<T> extends ActionStageResult<StartableContainer, T> {

        public PreStartResult(StartableContainer container, T content) {
            super(container, content);
        }

    }

    private static final class PostStartResult<T> extends ActionStageResult<StartedContainer, T> {

        public PostStartResult(StartedContainer container, T content) {
            super(container, content);
        }

    }

    private static class ContainerPreStartStage<U, V> implements LifecycleStage<PreStartResult<U>, PreStartResult<V>> {

        private final ProgressivePreStartContainerAction<U, V> innerStage;

        private ContainerPreStartStage(ProgressivePreStartContainerAction<U, V> innerStage) {
            this.innerStage = requireNonNull(innerStage);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ContainerPreStartStage.class.getSimpleName() + "[", "]")
                    .toString();
        }

        @Override
        public PreStartResult<V> commission(PreStartResult<U> requirement) throws Exception {
            V content = innerStage.perform(requirement.container, requirement.content);
            return new PreStartResult<>(requirement.container, content);
        }

        @Override
        public void decommission() {
            // no op
        }
    }

    private static class ContainerPostStartStage<U, V> implements LifecycleStage<PostStartResult<U>, PostStartResult<V>> {

        private final ProgressivePostStartContainerAction<U, V> action;

        private ContainerPostStartStage(ProgressivePostStartContainerAction<U, V> action) {
            this.action = action;
        }

        @Override
        public PostStartResult<V> commission(PostStartResult<U> requirement) throws Exception {
            V content = action.perform(requirement.container, requirement.content);
            return new PostStartResult<>(requirement.container, content);
        }

        @Override
        public void decommission() {
            // no op
        }
    }

    /**
     * Creates a new builder of container lifecycle instances.
     * @param constructor constructor of the {@link ContainerCreator} instance
     * @param parametry container creation parameters
     * @return a new builder
     */
    public static ProgressiveContainerLifecycleBuilder builder(ContainerCreatorConstructor ctor) {
        return new ProgressiveContainerLifecycleBuilderImpl(ctor);
    }

    public static ProgressiveContainerLifecycleBuilder buildGlobal() {
        ContainerCreatorConstructor ctor = new GlobalContainerCreatorConstructor(DjContainerCreator::new, clientConfig -> new DjManualContainerMonitor());
        return new ProgressiveContainerLifecycleBuilderImpl(ctor);
    }

    public static ProgressiveContainerLifecycleBuilder buildLocal() {
        ContainerCreatorConstructor ctor = new LocalContainerCreatorConstructor(DjContainerCreator::new, clientConfig -> new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(clientConfig).build()));
        return new ProgressiveContainerLifecycleBuilderImpl(ctor);
    }

    public interface FinishableLifecycleBuilder<P> {
        ProgressiveLifecycleStack<P> finish();
    }

    public interface ProgressiveContainerLifecycleBuilder {
        IndependentContainerActionAccumulator startedWith(ContainerParametry containerParametry);
    }

    public interface IndependentContainerActionAccumulator extends FinishableLifecycleBuilder<StartedContainer> {
        <P> ContainerActionAccumulator<P> pre(ProgressivePreStartContainerAction.IndependentPreStartAction<P> stage);
        <P> PostStartActionAccumulator<P> post(ProgressivePostStartContainerAction.IndependentPostStartAction<P> stage);
    }

    public interface ContainerActionAccumulator<P> extends FinishableLifecycleBuilder<P>, PostStartActionAccumulator<P> {
        <Q> ContainerActionAccumulator<Q> pre(ProgressivePreStartContainerAction<P, Q> stage);
    }

    public interface PostStartActionAccumulator<P> extends FinishableLifecycleBuilder<P> { // post
        <Q> PostStartActionAccumulator<Q> post(ProgressivePostStartContainerAction<P, Q> stage);
    }

    private static class ProgressiveContainerLifecycleBuilderImpl extends BuilderBase<ContainerCreator> implements ProgressiveContainerLifecycleBuilder {

        public ProgressiveContainerLifecycleBuilderImpl(ContainerCreatorConstructor ctor) {
            super(ProgressiveLifecycleStack.startingAt(new ContainerCreatorStage(ctor::instantiate)));
        }

        @Override
        public IndependentContainerActionAccumulator startedWith(ContainerParametry containerParametry) {
            return new Builder2Impl(stacker.andThen(new StartableContainerStage(containerParametry)));
        }
    }

    private static abstract class BuilderBase<T> {

        protected final ProgressiveLifecycleStack.Stacker<T> stacker;

        protected BuilderBase(ProgressiveLifecycleStack.Stacker<T> stacker) {
            this.stacker = requireNonNull(stacker);
        }

        protected static <T> LifecycleStage<PreStartResult<T>, PostStartResult<T>> transitionPreToPost() {
            DecoupledLifecycleStage.Commissioner<PreStartResult<T>, PostStartResult<T>> tCommissioner = new DecoupledLifecycleStage.Commissioner<PreStartResult<T>, PostStartResult<T>>() {
                @Override
                public PostStartResult<T> commission(PreStartResult<T> requirement) throws Exception {
                    StartedContainer startedContainer = requirement.container.start();
                    return new PostStartResult<>(startedContainer, requirement.content);
                }
            };
            DecoupledLifecycleStage.Decommissioner<PostStartResult<T>> tDecommissioner = AutoCloseableDecommissioner.byTransform(postStartResult -> postStartResult.container);
            return new DecoupledLifecycleStage<>(tCommissioner, tDecommissioner);
        }

        protected static LifecycleStage<StartableContainer, PreStartResult<Void>> transitionStartableToPre() {
            return new LifecycleStage<StartableContainer, PreStartResult<Void>>() {
                @Override
                public PreStartResult<Void> commission(StartableContainer requirement) {
                    return new PreStartResult<>(requirement, null);
                }

                @Override
                public void decommission() {
                }
            };
        }

        public static <U> LifecycleStage<PostStartResult<U>, U> transitionFinishing() {
            return new LifecycleStage<PostStartResult<U>, U>() {
                @Override
                public U commission(PostStartResult<U> requirement) {
                    return requirement.content;
                }

                @Override
                public void decommission() {
                }
            };
        }
    }

    private static class Builder2Impl extends BuilderBase<StartableContainer> implements IndependentContainerActionAccumulator {

        public Builder2Impl(ProgressiveLifecycleStack.Stacker<StartableContainer> stacker) {
            super(stacker);
        }

        @Override
        public ProgressiveLifecycleStack<StartedContainer> finish() {
            return stacker.andThen(new SimpleStartedContainerStage()).build();
        }

        @Override
        public <P> ContainerActionAccumulator<P> pre(ProgressivePreStartContainerAction.IndependentPreStartAction<P> stage) {
            ProgressiveLifecycleStack.Stacker<PreStartResult<Void>> transition = stacker.andThen(transitionStartableToPre());
            LifecycleStage<PreStartResult<Void>, PreStartResult<P>> stageWrapper = new ContainerPreStartStage<>(stage);
            ProgressiveLifecycleStack.Stacker<PreStartResult<P>> pStacker = transition.andThen(stageWrapper);
            return new Builder3Impl<>(pStacker);
        }

        @Override
        public <P> PostStartActionAccumulator<P> post(ProgressivePostStartContainerAction.IndependentPostStartAction<P> stage) {
            return new Builder4Impl<>(stacker
                .andThen(transitionStartableToPre())
                .andThen(transitionPreToPost())
                .andThen(new ContainerPostStartStage<>(stage))
            );
        }
    }

    private static class Builder3Impl<T> extends BuilderBase<PreStartResult<T>> implements ContainerActionAccumulator<T> {

        public Builder3Impl(ProgressiveLifecycleStack.Stacker<PreStartResult<T>> stacker) {
            super(stacker);
        }

        @Override
        public ProgressiveLifecycleStack<T> finish() {
            return stacker.andThen(transitionPreToPost())
                          .andThen(transitionFinishing()).build();
        }

        @Override
        public <Q> ContainerActionAccumulator<Q> pre(ProgressivePreStartContainerAction<T, Q> stage) {
            return new Builder3Impl<>(stacker.andThen(new ContainerPreStartStage<>(stage)));
        }

        @Override
        public <Q> PostStartActionAccumulator<Q> post(ProgressivePostStartContainerAction<T, Q> stage) {
            return new Builder4Impl<>(stacker
                    .andThen(transitionPreToPost())
                    .andThen(new ContainerPostStartStage<>(stage)));
        }
    }

    private static class Builder4Impl<T> extends BuilderBase<PostStartResult<T>> implements PostStartActionAccumulator<T> {

        public Builder4Impl(ProgressiveLifecycleStack.Stacker<PostStartResult<T>> b) {
            super(b);
        }

        @Override
        public ProgressiveLifecycleStack<T> finish() {
            return stacker.andThen(BuilderBase.transitionFinishing()).build();
        }

        @Override
        public <Q> PostStartActionAccumulator<Q> post(ProgressivePostStartContainerAction<T, Q> stage) {
            return new Builder4Impl<>(stacker.andThen(new ContainerPostStartStage<>(stage)));
        }
    }

    @Override
    public String toString() {
        return String.format("Container%s", super.toString());
    }
}
