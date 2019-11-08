package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
public class ContainerLifecycles {

    private ContainerLifecycles() {}

    private static class ContainerRunnerLifecycle extends DecoupledLifecycle<ContainerCreator> {
        public ContainerRunnerLifecycle(Commissioner<ContainerCreator> commissioner) {
            super(commissioner, new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("ContainerRunnerLifecycle@%08x", hashCode());
        }
    }

    private static class RunnableContainerLifecycle extends DecoupledLifecycle<StartableContainer> {

        public RunnableContainerLifecycle(Commissioner<StartableContainer> commissioner) {
            super(commissioner, new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("RunnableContainerLifecycle@%08x", hashCode());
        }
    }

    private static class RunningContainerLifecycle extends DecoupledLifecycle<StartedContainer> {

        public RunningContainerLifecycle(Commissioner<StartedContainer> commissioner) {
            super(commissioner, new AutoCloseableDecommissioner<>());
        }

        @Override
        public String toString() {
            return String.format("RunningContainerLifecycle@%08x", hashCode());
        }
    }

    private static class PreStartActionExecutor implements Lifecycle<Integer> {

        private final Supplier<? extends StartableContainer> containerSupplier;
        private final List<? extends ContainerAction> preStartActions;

        public PreStartActionExecutor(Supplier<? extends StartableContainer> containerSupplier, List<? extends ContainerAction> preStartActions) {
            this.containerSupplier = containerSupplier;
            this.preStartActions = preStartActions;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PreStartActionExecutor.class.getSimpleName() + "[", "]")
                    .add("containerSupplier=" + containerSupplier)
                    .add("preStartActions=" + preStartActions)
                    .toString();
        }

        @Override
        public Integer commission() throws ContainmentException {
            StartableContainer runnable = containerSupplier.get();
            for (ContainerAction action : preStartActions) {
                action.perform(runnable);
            }
            return preStartActions.size();
        }

        @Override
        public void decommission() {
            // no op
        }
    }

    private static class PostStartActionExecutor implements Lifecycle<StartedContainer> {

        private final Supplier<? extends StartedContainer> containerSupplier;
        private final List<? extends StartedContainerAction> postStartActions;

        public PostStartActionExecutor(Supplier<? extends StartedContainer> containerSupplier, List<? extends StartedContainerAction> postStartActions) {
            this.containerSupplier = containerSupplier;
            this.postStartActions = postStartActions;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PreStartActionExecutor.class.getSimpleName() + "[", "]")
                    .add("containerSupplier=" + containerSupplier)
                    .add("postStartActions=" + postStartActions)
                    .toString();
        }

        @Override
        public StartedContainer commission() throws Exception {
            StartedContainer container = containerSupplier.get();
            for (StartedContainerAction action : postStartActions) {
                if (container == null) {
                    container = containerSupplier.get();
                }
                action.perform(container);
            }
            return container;
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
    public static Builder builder(ContainerParametry parametry) {
        return new Builder(parametry);
    }

    /**
     * Builder of container lifecycle instances.
     */
    public static class Builder {

        private final ContainerParametry parametry;
        private final List<ContainerAction> preStartActions;
        private final List<StartedContainerAction> postStartActions;

        private Builder(ContainerParametry parametry) {
            this.parametry = requireNonNull(parametry);
            preStartActions = new ArrayList<>();
            postStartActions = new ArrayList<>();
        }

        /**
         * Adds a pre-start action.
         * @param action action
         * @return this builder instance
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder preStart(ContainerAction action) {
            preStartActions.add(action);
            return this;
        }

        /**
         * Adds a post-start action.
         * @param action action
         * @return this builder instance
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder postStart(StartedContainerAction action) {
            postStartActions.add(action);
            return this;
        }

        /**
         * Creates a container lifecycle instance.
         * @return a new lifecycle instance
         */
        public Lifecycle<StartedContainer> build(ContainerCreatorConstructor constructor) {
            return create(constructor, parametry, preStartActions, postStartActions);
        }
    }

    private static Lifecycle<StartedContainer> create(ContainerCreatorConstructor constructor, ContainerParametry parametry, List<? extends ContainerAction> preStartActions, List<? extends StartedContainerAction> postStartActions) {
        AtomicReference<ContainerCreator> runnerRef = new AtomicReference<>();
        Lifecycle<ContainerCreator> runnerLifecycle = new ContainerRunnerLifecycle(() -> {
            ContainerCreator runner = constructor.instantiate();
            runnerRef.set(runner);
            return runner;
        });
        AtomicReference<StartableContainer> runnableRef = new AtomicReference<>();
        Lifecycle<StartableContainer> runnableLifecycle = new RunnableContainerLifecycle(() -> {
            ContainerCreator runner = runnerRef.get();
            StartableContainer runnable = runner.create(parametry);
            runnableRef.set(runnable);
            return runnable;
        });
        Lifecycle<Integer> preStartActionLifecycle = new PreStartActionExecutor(runnableRef::get, preStartActions);
        AtomicReference<StartedContainer> runningRef = new AtomicReference<>();
        Lifecycle<StartedContainer> runningLifecycle = new RunningContainerLifecycle(() -> {
            StartableContainer runnable = runnableRef.get();
            StartedContainer container = runnable.start();
            runningRef.set(container);
            return container;
        });
        Lifecycle<StartedContainer> postStartActionsLifecycle = new PostStartActionExecutor(runningRef::get, postStartActions);
        return LifecycleStack.builder()
                .addStage(runnerLifecycle)
                .addStage(runnableLifecycle)
                .addStage(preStartActionLifecycle)
                .addStage(runningLifecycle)
                .finish(postStartActionsLifecycle);
    }

    private static class AutoCloseableDecommissioner<T extends AutoCloseable> implements DecoupledLifecycle.Decommissioner<T> {

        @Override
        public void decommission(T resource) {
            try {
                resource.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new WrappedAutocloseException(e);
            }
        }

        public static class WrappedAutocloseException extends RuntimeException {
            public WrappedAutocloseException(Exception cause) {
                super(cause);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Container%s", super.toString());
    }
}
