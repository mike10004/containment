package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ActionableContainer;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.RunningContainer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class LifecycledResourceBuilderTest {

    private static class ContainerPlus<T> {
        public final RunningContainer container;
        public final T annotation;

        public ContainerPlus(RunningContainer container, T annotation) {
            this.container = container;
            this.annotation = annotation;
        }
    }

    @Test
    public void buildLocalDependency() throws Exception {
        List<LifecycleEvent> events = Collections.synchronizedList(new ArrayList<>());
        ContainerParametry parametry = ContainerParametry.builder(Tests.getAlpineImage())
                .commandToWaitIndefinitely()
                .build();
        Lifecycle<ContainerPlus<String>> stack = ContainerLifecycles.buildLocal()
                .creating(parametry)
                .pre(new ContainerInitialPreStartAction<String>() {
                    @Override
                    public String perform(ActionableContainer container) throws Exception {
                        return "A";
                    }
                }).pre((container, input) -> input + "B")
                .post((container, input) -> input + "C")
                .post((container, input) -> input + "D")
                .post(ContainerPlus::new)
                .finish();
        LifecycledResource<ContainerPlus<String>> dependency = new LifecycledResourceBuilder()
                .eventListener(events::add)
                .buildResource(stack);
        ContainerPlus<String> containerPlus = dependency.request().require();
        try {
            RunningContainer container = containerPlus.container;
            ContainerSubprocessResult<String> result1 = container.executor().execute("echo", "hello, world");
            System.out.println(result1);
            assertEquals("exit", 0, result1.exitCode());
            assertEquals("text", "hello, world", result1.stdout().trim());
        } finally {
            dependency.finishLifecycle();
            // TODO check for actual stoppage and removal
        }
        Set<LifecycleEvent.Category> eventCats = events.stream().map(LifecycleEvent::getCategory).collect(Collectors.toSet());
        assertEquals("events", EnumSet.of(
                LifecycleEvent.Category.COMMISSION_STARTED,
                LifecycleEvent.Category.COMMISSION_SUCCEEDED,
                LifecycleEvent.Category.PROVIDE_STARTED,
                LifecycleEvent.Category.PROVIDE_COMPLETED,
                LifecycleEvent.Category.FINISH_STARTED,
                LifecycleEvent.Category.FINISH_COMPLETED
        ), eventCats);
        assertEquals("annotation", "ABCD", containerPlus.annotation);
    }
}