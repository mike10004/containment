package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Class that contains utility methods for building container lifecycles.
 *
 * A container's lifecycle
 * includes the following stages:
 * <ul>
 *     <li><b>Commission</b>
 *       <ul>
 *         <li>create</li>
 *         <li>execute pre-start actions</li>
 *         <li>start</li>
 *         <li>execute post-start actions</li>
 *       </ul>
 *     </li>
 *     <li><b>Decommission</b>
 *       <ul>
 *         <li>stop</li>
 *         <li>remove</li>
 *       </ul>
 *     </li>
 * </ul>
 */
public class ProgressiveLifecycles {

    private ProgressiveLifecycles() {}

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

        private final PreStartContainerStage<U, V> innerStage;

        private ContainerPreStartStage(PreStartContainerStage<U, V> innerStage) {
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

        private final PostStartContainerStage<U, V> action;

        private ContainerPostStartStage(PostStartContainerStage<U, V> action) {
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
    public static Builder1 builder(ContainerCreatorConstructor ctor) {
        return new Builder1Impl(ctor);
    }

    public interface Finishable<P> {
        LifecycleStageStack<P> finish();
    }

    public interface Builder2 extends Finishable<StartedContainer> {
        <P> Builder3<P> pre(PreStartContainerStageOne<P> stage);
        <P> Builder4<P> post(PostStartContainerStageOne<P> stage);
    }

    public interface Builder4<P> extends Finishable<P> { // post
        <Q> Builder4<Q> post(PostStartContainerStage<P, Q> stage);
    }

    public interface Builder3<P> extends Finishable<P> {
        <Q> Builder3<Q> pre(PreStartContainerStage<P, Q> stage);
        <Q> Builder4<Q> post(PostStartContainerStage<P, Q> stage);
    }

    public interface Builder1 {
        Builder2 startedWith(ContainerParametry containerParametry);
    }

    private static class Builder1Impl extends BuilderBase<ContainerCreator> implements Builder1 {

        public Builder1Impl(ContainerCreatorConstructor ctor) {
            super(LifecycleStageStack.builder(new ContainerCreatorStage(ctor::instantiate)));
        }

        @Override
        public Builder2 startedWith(ContainerParametry containerParametry) {
            return new Builder2Impl(builder.addStage(new StartableContainerStage(containerParametry)));
        }
    }

    private static abstract class BuilderBase<T> {

        protected final LifecycleStageStack.Builder<T> builder;

        protected BuilderBase(LifecycleStageStack.Builder<T> builder) {
            this.builder = requireNonNull(builder);
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

    private static class Builder2Impl extends BuilderBase<StartableContainer> implements Builder2 {

        public Builder2Impl(LifecycleStageStack.Builder<StartableContainer> builder) {
            super(builder);
        }

        @Override
        public LifecycleStageStack<StartedContainer> finish() {
            return builder.addStage(new SimpleStartedContainerStage()).build();
        }

        @Override
        public <P> Builder3<P> pre(PreStartContainerStageOne<P> stage) {
            LifecycleStageStack.Builder<PreStartResult<Void>> transition = builder.addStage(transitionStartableToPre());
            LifecycleStage<PreStartResult<Void>, PreStartResult<P>> stageWrapper = new ContainerPreStartStage<>(stage);
            LifecycleStageStack.Builder<PreStartResult<P>> pBuilder = transition.addStage(stageWrapper);
            return new Builder3Impl<>(pBuilder);
        }

        @Override
        public <P> Builder4<P> post(PostStartContainerStageOne<P> stage) {
            return new Builder4Impl<>(builder
                .addStage(transitionStartableToPre())
                .addStage(transitionPreToPost())
                .addStage(new ContainerPostStartStage<>(stage))
            );
        }
    }

    private static class Builder3Impl<T> extends BuilderBase<PreStartResult<T>> implements Builder3<T> {

        public Builder3Impl(LifecycleStageStack.Builder<PreStartResult<T>> builder) {
            super(builder);
        }

        @Override
        public LifecycleStageStack<T> finish() {
            return builder.addStage(transitionPreToPost())
                          .addStage(transitionFinishing()).build();
        }

        @Override
        public <Q> Builder3<Q> pre(PreStartContainerStage<T, Q> stage) {
            return new Builder3Impl<>(builder.addStage(new ContainerPreStartStage<>(stage)));
        }

        @Override
        public <Q> Builder4<Q> post(PostStartContainerStage<T, Q> stage) {
            return new Builder4Impl<>(builder
                    .addStage(transitionPreToPost())
                    .addStage(new ContainerPostStartStage<>(stage)));
        }
    }

    private static class Builder4Impl<T> extends BuilderBase<PostStartResult<T>> implements Builder4<T> {

        public Builder4Impl(LifecycleStageStack.Builder<PostStartResult<T>> b) {
            super(b);
        }

        @Override
        public LifecycleStageStack<T> finish() {
            return builder.addStage(BuilderBase.transitionFinishing()).build();
        }

        @Override
        public <Q> Builder4<Q> post(PostStartContainerStage<T, Q> stage) {
            return new Builder4Impl<>(builder.addStage(new ContainerPostStartStage<>(stage)));
        }
    }

    @Override
    public String toString() {
        return String.format("Container%s", super.toString());
    }
}
