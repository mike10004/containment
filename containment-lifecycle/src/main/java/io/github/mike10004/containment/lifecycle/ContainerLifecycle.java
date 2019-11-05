package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerAction;
import io.github.mike10004.containment.RunnableContainer;
import io.github.mike10004.containment.RunningContainer;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ContainerLifecycle extends LifecycleStack<RunningContainer> {

    private ContainerLifecycle(Iterable<? extends Lifecycle<?>> others, Lifecycle<RunningContainer> top) {
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

    private static class RunnableContainerLifecycle extends DecoupledLifecycle<RunnableContainer> {

        public RunnableContainerLifecycle(Commissioner<RunnableContainer> commissioner) {
            super(commissioner, new RuntimeExceptionWrapper<>());
        }

        @Override
        public String toString() {
            return String.format("RunnableContainerLifecycle@%08x", hashCode());
        }
    }

    private static class RunningContainerLifecycle extends DecoupledLifecycle<RunningContainer> {

        public RunningContainerLifecycle(Commissioner<RunningContainer> commissioner) {
            super(commissioner, new RuntimeExceptionWrapper<>());
        }

        @Override
        public String toString() {
            return String.format("RunningContainerLifecycle@%08x", hashCode());
        }
    }

    private static class PreStartActionExecutor implements Lifecycle<Integer> {

        private final Supplier<? extends RunnableContainer> containerSupplier;
        private final List<? extends ContainerAction> preStartActions;

        public PreStartActionExecutor(Supplier<? extends RunnableContainer> containerSupplier, List<? extends ContainerAction> preStartActions) {
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
            RunnableContainer runnable = containerSupplier.get();
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

    public static ContainerLifecycle create(ContainerRunnerConstructor constructor, ContainerParametry parametry, List<? extends ContainerAction> preStartActions) {
        AtomicReference<ContainerCreator> runnerRef = new AtomicReference<>();
        Lifecycle<ContainerCreator> runnerLifecycle = new ContainerRunnerLifecycle(() -> {
            ContainerCreator runner = constructor.instantiate();
            runnerRef.set(runner);
            return runner;
        });
        AtomicReference<RunnableContainer> runnableRef = new AtomicReference<>();
        Lifecycle<RunnableContainer> runnableLifecycle = new RunnableContainerLifecycle(() -> {
            ContainerCreator runner = runnerRef.get();
            RunnableContainer runnable = runner.create(parametry);
            runnableRef.set(runnable);
            return runnable;
        });
        Lifecycle<Integer> preStartActionLifecycle = new PreStartActionExecutor(runnableRef::get, preStartActions);
        Lifecycle<RunningContainer> runningLifecycle = new RunningContainerLifecycle(() -> {
            RunnableContainer runnable = runnableRef.get();
            return runnable.start();
        });
        return new ContainerLifecycle(Arrays.asList(runnerLifecycle, runnableLifecycle, preStartActionLifecycle), runningLifecycle);
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
}
