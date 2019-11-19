package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.lifecycle.LifecycledResource;
import io.github.mike10004.containment.lifecycle.LifecycleEvent;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;

public abstract class SharedGlobalContainerRuleTestBase {

    @Test
    public void test1() throws Exception {
        ContainerInfo info = getContainerDependency().request().require().info();
        Common.assertContainerAlive(info);
    }

    @Test
    public void test2() throws Exception {
        ContainerInfo info = getContainerDependency().request().require().info();
        Common.assertContainerAlive(info);
    }

    protected final LifecycledResource<RunningContainer> getContainerDependency() {
        return Globals.GLOBAL_BUSYBOX_RULE;
    }

    protected static void checkEventsOnTearDown() {
        List<LifecycleEvent.Category> events = Globals.GLOBAL_BUSYBOX_RULE_COLLECTOR.categories();
        assertFalse("expect zero FINISH events", events.contains(LifecycleEvent.Category.FINISH_STARTED));
        assertFalse("expect zero FINISH events", events.contains(LifecycleEvent.Category.FINISH_COMPLETED));
    }
}
