package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.lifecycle.ContainerDependency;
import io.github.mike10004.containment.lifecycle.LifecycleEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocalSingleMethodContainerRuleTest {

    private final EventCollector listener;

    @Rule
    public final RuleChain ruleChain;

    private final ContainerDependencyRule containerRule;

    public LocalSingleMethodContainerRuleTest() {
        listener = new EventCollector();
        containerRule = new ContainerDependencyRule(ContainerDependency.builder(parametry()).eventListener(this.listener).buildLocalDependency());
        TestRule eventCheckRule = new ExternalResource() {

            @Override
            protected void before() {
                listener.events.clear();
            }

            @Override
            protected void after() {
                List<LifecycleEvent.Category> categories = listener.categories();
                assertEquals("events after container rule teardown", Arrays.asList(
                        LifecycleEvent.Category.PROVIDE_STARTED,
                        LifecycleEvent.Category.COMMISSION_STARTED,
                        LifecycleEvent.Category.COMMISSION_SUCCEEDED,
                        LifecycleEvent.Category.PROVIDE_COMPLETED,
                        LifecycleEvent.Category.FINISH_STARTED,
                        LifecycleEvent.Category.FINISH_COMPLETED
                        ), categories);
            }
        };
        ruleChain = RuleChain.outerRule(eventCheckRule).around(containerRule);
    }

    private static ContainerParametry parametry() {
        return ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
    }

    @Test
    public void tryIt_firstTime() throws Exception {
        doIt();
    }

    @Test
    public void tryIt_Again() throws Exception {
        doIt();
    }

    private void doIt() throws Exception {
        assertEquals("events before container() invoked", Collections.emptyList(), listener.events);
        ContainerInfo info = containerRule.container().info();
        Common.assertContainerAlive(info);
        assertEquals("events before existing test method", Arrays.asList(
                LifecycleEvent.Category.PROVIDE_STARTED,
                LifecycleEvent.Category.COMMISSION_STARTED,
                LifecycleEvent.Category.COMMISSION_SUCCEEDED,
                LifecycleEvent.Category.PROVIDE_COMPLETED
                ), listener.categories());

    }
}