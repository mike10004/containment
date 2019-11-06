package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ContainerLifecycle extends LifecycleStack<StartedContainer> {

    private ContainerLifecycle(Iterable<? extends Lifecycle<?>> others, Lifecycle<StartedContainer> top) {
        super(others, top);
    }

    private static class ContainerRunnerLifecycle extends DecoupledLifecycle<ContainerCreator> {
        public ContainerRunnerLifecycle(Commissioner<ContainerCreator> commissioner) {
            super(commissioner, new RuntimeExceptionWrapper<>());
        }

        @Override
        public String toString() {
            return String.format("ContainerRunnerLifecycle@%08x", hashCode());
        }
    }

    private static class RunnableContainerLifecycle extends DecoupledLifecycle<StartableContainer> {

        public RunnableContainerLifecycle(Commissioner<StartableContainer> commissioner) {
            super(commissioner, new RuntimeExceptionWrapper<>());
        }

        @Override
        public String toString() {
            return String.format("RunnableContainerLifecycle@%08x", hashCode());
        }
    }

    private static class RunningContainerLifecycle extends DecoupledLifecycle<StartedContainer> {

        public RunningContainerLifecycle(Commissioner<StartedContainer> commissioner) {
            super(commissioner, new RuntimeExceptionWrapper<>());
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
        public Integer commission() throws Exception {
            StartableContainer runnable = containerSupplier.get();
            for (ContainerAction action : preStartActions) {
                runnable.execute(action);
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
        private final List<? extends RunningContainerAction> postStartActions;

        public PostStartActionExecutor(Supplier<? extends StartedContainer> containerSupplier, List<? extends RunningContainerAction> postStartActions) {
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
            for (RunningContainerAction action : postStartActions) {
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

    public static ContainerLifecycle create(ContainerRunnerConstructor constructor, ContainerParametry parametry, List<? extends ContainerAction> preStartActions, List<? extends RunningContainerAction> postStartActions) {
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
        return new ContainerLifecycle(Arrays.asList(runnerLifecycle, runnableLifecycle, preStartActionLifecycle, runningLifecycle), postStartActionsLifecycle);
    }

    private static class RuntimeExceptionWrapper<T extends AutoCloseable> implements DecoupledLifecycle.Decommissioner<T> {

        @Override
        public void decommission(T value) {
            try {
                value.close();
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
