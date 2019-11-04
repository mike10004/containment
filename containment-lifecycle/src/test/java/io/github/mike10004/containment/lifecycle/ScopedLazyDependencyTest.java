package io.github.mike10004.containment.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ScopedLazyDependencyTest {

    @org.junit.Test
    public void provide() {
        ObjectCommissioner commissioner = new ObjectCommissioner();
        ObjectDecommissioner decommissioner = new ObjectDecommissioner();
        Widget value;
        try (ScopedLazyDependency<Widget> dependency = new ScopedLazyDependency<>(new DecoupledLifecycle<>(commissioner, decommissioner))) {
            Provision<Widget> provision = dependency.provide();
            assertTrue("expect succeeded: " + provision, provision.isSucceeded());
            value = provision.require();
            assertEquals("expect exactly one commissioning", Collections.singletonList(value), commissioner.commissioned);
            Widget valueAgain = dependency.provide().require();
            assertSame(value, valueAgain);
            assertEquals("expect exactly one commissioning after re-getting", Collections.singletonList(valueAgain), commissioner.commissioned);
        }
        assertEquals(Collections.singletonList(value), commissioner.commissioned);
        assertEquals(Collections.singletonList(value), decommissioner.decommissioned);
    }

    private static final class Widget {
    }

    private static class ObjectCommissioner implements DecoupledLifecycle.Commissioner<Widget> {

        public final List<Widget> commissioned = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Widget commission() throws Exception {
            Widget value = new Widget();
            commissioned.add(value);
            return value;
        }

    }

    private static class ObjectDecommissioner implements DecoupledLifecycle.Decommissioner<Widget> {

        public final List<Widget> decommissioned = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void decommission(Widget value) {
            decommissioned.add(value);
        }
    }
}