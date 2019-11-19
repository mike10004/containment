package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.lifecycle.ContainerLifecycles;
import io.github.mike10004.containment.lifecycle.Lifecycle;
import io.github.mike10004.containment.lifecycle.LifecycleEvent;
import io.github.mike10004.containment.lifecycle.LifecycledResource;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocalClassContainerRuleTest {

    private static final EventCollector listener;

    private static final ContainerDependencyRule containerRule;

    @ClassRule
    public static final RuleChain ruleChain;

    static {
        listener = new EventCollector();
        ContainerParametry parametry = ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
        Lifecycle<StartedContainer> lifecycle = ContainerLifecycles.buildLocal().creating(parametry).finish();
        LifecycledResource<StartedContainer> resource = LifecycledResource.builder()
                .eventListener(listener)
                .buildLocalResource(lifecycle);
        containerRule = new ContainerDependencyRule(resource);
        TestRule eventCheckRule = new ExternalResource() {

            @Override
            protected void before() {
                listener.events.clear();
            }

            @Override
            protected void after() {
                List<LifecycleEvent.Category> categories = listener.categories();
                assertEquals("events after container rule teardown", Arrays.asList(

                        // events during first test
                        LifecycleEvent.Category.PROVIDE_STARTED,
                        LifecycleEvent.Category.COMMISSION_STARTED,
                        LifecycleEvent.Category.COMMISSION_SUCCEEDED,
                        LifecycleEvent.Category.PROVIDE_COMPLETED,

                        // events during second test
                        LifecycleEvent.Category.PROVIDE_STARTED,
                        LifecycleEvent.Category.PROVIDE_COMPLETED,

                        // events during teardown
                        LifecycleEvent.Category.FINISH_STARTED,
                        LifecycleEvent.Category.FINISH_COMPLETED
                        ), categories);
            }
        };
        ruleChain = RuleChain.outerRule(eventCheckRule).around(containerRule);
    }

    @Test
    public void tryIt1() throws Exception {
        doIt();
    }

    @Test
    public void tryIt2() throws Exception {
        doIt();
    }

    private void doIt() throws Exception {
        ContainerInfo info = containerRule.container().info();
        Common.assertContainerAlive(info);
    }
}