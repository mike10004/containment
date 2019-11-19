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
 * Class that contains utility methods for building container lifecycles.
 */
public class ContainerLifecycles {

    private ContainerLifecycles() {}

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

        private final ContainerPreStartAction<U, V> innerStage;

        public ContainerPreStartStage(ContainerPreStartAction<U, V> innerStage) {
            this.innerStage = requireNonNull(innerStage);
        }

        public ContainerPreStartStage(ContainerInitialPreStartAction<V> innerStage) {
            this((container, requirement) -> innerStage.perform(container));
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

        private final ContainerPostStartAction<U, V> action;

        public ContainerPostStartStage(ContainerPostStartAction<U, V> action) {
            this.action = action;
        }

        public ContainerPostStartStage(ContainerInitialPostStartAction<V> action) {
            this((container, requirement) -> action.perform(container));
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
     * @param ctor constructor of the {@link ContainerCreator} instance
     * @return a new builder
     */
    public static PreCreate builder(ContainerCreatorFactory ctor) {
        return new PreCreateImpl(ctor);
    }

    public static PreCreate buildGlobal() {
        ContainerCreatorFactory ctor = new GlobalContainerCreatorFactory(DjContainerCreator::new, clientConfig -> new DjManualContainerMonitor());
        return new PreCreateImpl(ctor);
    }

    public static PreCreate buildLocal() {
        ContainerCreatorFactory ctor = new LocalContainerCreatorFactory(DjContainerCreator::new, clientConfig -> new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(clientConfig).build()));
        return new PreCreateImpl(ctor);
    }

    /**
     * Parent interface for services that can produce complete lifecycle instances.
     * @param <P> resource type
     */
    public interface LifecycleFinisher<P> {
        Lifecycle<P> finish();
        Lifecycle<StartedContainer> finishWithContainer();
    }

    /**
     * Interface of a service that allows definition of container parametry
     * when building a container lifecycle.
     */
    public interface PreCreate {
        PreStartInitial creating(ContainerParametry containerParametry);
    }

    /**
     * Interface of a service that supports defining actions that can be defined
     * in the pre-start stage before any other actions have been defined.
     */
    public interface PreStartInitial extends LifecycleFinisher<StartedContainer> {
        <P> PreStartSubsequent<P> pre(ContainerInitialPreStartAction<P> action);
        PreStartSubsequent<Void> runPre(ContainerPreStartRunnable runnable);
        <P> PostStart<P> post(ContainerInitialPostStartAction<P> action);
        PostStart<Void> runPost(ContainerPostStartRunnable runnable);
    }

    /**
     * Interface of a service that supports defining actions that can be defined
     * in the pre-start stage
     * @param <P> resource type produced if lifecycle were finished now
     */
    public interface PreStartSubsequent<P> extends LifecycleFinisher<P> {
        <Q> PreStartSubsequent<Q> pre(ContainerPreStartAction<P, Q> action);
        PreStartSubsequent<P> runPre(ContainerPreStartRunnable runnable);
        <Q> PostStart<Q> post(ContainerPostStartAction<P, Q> action);
        PostStart<P> runPost(ContainerPostStartRunnable runnable);
    }

    /**
     * Interface of a service that supports defining post-start actions.
     * @param <P> resource type produced if lifecycle were finished now
     */
    public interface PostStart<P> extends LifecycleFinisher<P> { // post
        <Q> PostStart<Q> post(ContainerPostStartAction<P, Q> action);
        PostStart<P> runPost(ContainerPostStartRunnable runnable);
    }

    private static class PreCreateImpl extends BuilderBase<ContainerCreator> implements PreCreate {

        public PreCreateImpl(ContainerCreatorFactory ctor) {
            super(LifecycleStack.startingAt(new ContainerCreatorStage(ctor::instantiate)));
        }

        @Override
        public PreStartInitial creating(ContainerParametry containerParametry) {
            return new Builder2Impl(stacker.andThen(new StartableContainerStage(containerParametry)));
        }
    }

    private static abstract class BuilderBase<T> {

        protected final LifecycleStackLink<T> stacker;

        protected BuilderBase(LifecycleStackLink<T> stacker) {
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

    private static class Builder2Impl extends BuilderBase<StartableContainer> implements PreStartInitial {

        public Builder2Impl(LifecycleStackLink<StartableContainer> stacker) {
            super(stacker);
        }

        @Override
        public Lifecycle<StartedContainer> finish() {
            return finishWithContainer();
        }

        @Override
        public PreStartSubsequent<Void> runPre(ContainerPreStartRunnable runnable) {
            return pre(runnable.asInitialAction());
        }

        @Override
        public PostStart<Void> runPost(ContainerPostStartRunnable runnable) {
            return post(runnable.asInitialAction());
        }

        @Override
        public <P> PreStartSubsequent<P> pre(ContainerInitialPreStartAction<P> action) {
            LifecycleStackLink<PreStartResult<Void>> transition = stacker.andThen(transitionStartableToPre());
            LifecycleStage<PreStartResult<Void>, PreStartResult<P>> stageWrapper = new ContainerPreStartStage<>(action);
            LifecycleStackLink<PreStartResult<P>> pStacker = transition.andThen(stageWrapper);
            return new Builder3Impl<>(pStacker);
        }

        @Override
        public <P> PostStart<P> post(ContainerInitialPostStartAction<P> action) {
            return new Builder4Impl<>(stacker
                .andThen(transitionStartableToPre())
                .andThen(transitionPreToPost())
                .andThen(new ContainerPostStartStage<>(action))
            );
        }

        @Override
        public Lifecycle<StartedContainer> finishWithContainer() {
            return stacker.andThen(new SimpleStartedContainerStage()).toSequence();
        }
    }

    private static class Builder3Impl<T> extends BuilderBase<PreStartResult<T>> implements PreStartSubsequent<T> {

        public Builder3Impl(LifecycleStackLink<PreStartResult<T>> stacker) {
            super(stacker);
        }

        @Override
        public Lifecycle<T> finish() {
            return stacker.andThen(transitionPreToPost())
                          .andThen(transitionFinishing()).toSequence();
        }

        @Override
        public Lifecycle<StartedContainer> finishWithContainer() {
            return post((container, x) -> container).finish();
        }

        @Override
        public PostStart<T> runPost(ContainerPostStartRunnable runnable) {
            return post(runnable.asPassThru());
        }

        @Override
        public PreStartSubsequent<T> runPre(ContainerPreStartRunnable runnable) {
            return pre(runnable.asPassThru());
        }

        @Override
        public <Q> PreStartSubsequent<Q> pre(ContainerPreStartAction<T, Q> action) {
            return new Builder3Impl<>(stacker.andThen(new ContainerPreStartStage<>(action)));
        }

        @Override
        public <Q> PostStart<Q> post(ContainerPostStartAction<T, Q> action) {
            return new Builder4Impl<>(stacker
                    .andThen(transitionPreToPost())
                    .andThen(new ContainerPostStartStage<>(action)));
        }
    }

    private static class Builder4Impl<T> extends BuilderBase<PostStartResult<T>> implements PostStart<T> {

        public Builder4Impl(LifecycleStackLink<PostStartResult<T>> b) {
            super(b);
        }

        @Override
        public Lifecycle<T> finish() {
            return stacker.andThen(BuilderBase.transitionFinishing()).toSequence();
        }

        @Override
        public <Q> PostStart<Q> post(ContainerPostStartAction<T, Q> action) {
            return new Builder4Impl<>(stacker.andThen(new ContainerPostStartStage<>(action)));
        }

        @Override
        public PostStart<T> runPost(ContainerPostStartRunnable runnable) {
            return post(runnable.asPassThru());
        }

        @Override
        public Lifecycle<StartedContainer> finishWithContainer() {
            return post((container, requirement) -> container).finish();
        }
    }

    @Override
    public String toString() {
        return String.format("Container%s", super.toString());
    }
}
