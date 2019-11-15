package io.github.mike10004.containment.lifecycle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class SimpleLifecycleStackBuilderTest {

    private static Lifecycle<Widget> createStack(List<WidgetLifecycle> others, WidgetLifecycle finalStage) {
        SimpleLifecycleStackBuilder b = SimpleLifecycleStackBuilder.create();
        for (WidgetLifecycle lifecycle : others) {
            b = b.addStage(lifecycle);
        }
        return b.finish(finalStage);
    }

    @Test
    public void gardenPath() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new WidgetLifecycle(tracker), second = new WidgetLifecycle(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first, second);
        WidgetLifecycle third = new WidgetLifecycle(tracker);
        Lifecycle<Widget> stack = createStack(others, third);
        Widget topWidget = stack.commission();
        assertNotNull(topWidget);
        stack.decommission();
        assertEquals(1, first.commissioned);
        assertEquals(2, second.commissioned);
        assertEquals(3, third.commissioned);
        assertEquals(1, third.decommissioned);
        assertEquals(2, second.decommissioned);
        assertEquals(3, first.decommissioned);
    }

    @Test
    public void lastCommissionFailsAndMiddleDecommissionFails() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new WidgetLifecycle(tracker), second = new ErrorOnDecommission(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first, second);
        WidgetLifecycle third = new ErrorOnCommission(tracker);
        Lifecycle<Widget> stack = createStack(others, third);
        try {
            stack.commission();
            fail("should have excepted");
        } catch (ProgressiveLifecycleStackCommissionUnwindException e) {
            assertTrue(e.commissionExceptionThrower.isEquivalent(third));
            assertSame(e.commissionException.getClass(), TestCommissionException.class);
            assertEquals(1, e.unwindException.exceptionsThrown.size());
            Map.Entry<LifecycleStage<?, ?>, RuntimeException> entry = e.unwindException.exceptionsThrown.entrySet().iterator().next();
            assertTrue(entry.getKey().isEquivalent(second));
            assertEquals(entry.getValue().getClass(), TestRuntimeDecommissionException.class);
        }
        assertEquals(1, first.commissioned);
        assertEquals(2, second.commissioned);
        assertEquals(0, third.commissioned);
        assertEquals(0, third.decommissioned);
        assertEquals(0, second.decommissioned);
        assertEquals(1, first.decommissioned);
    }

    @Test
    public void lastCommissionFails() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new WidgetLifecycle(tracker), second = new WidgetLifecycle(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first, second);
        WidgetLifecycle third = new ErrorOnCommission(tracker);
        Lifecycle<Widget> stack = createStack(others, third);
        try {
            stack.commission();
            fail("should have excepted");
        } catch (ProgressiveLifecycleStackCommissionException e) {
            assertTrue(e.getCause() instanceof TestCommissionException);
        }
        assertEquals(1, first.commissioned);
        assertEquals(2, second.commissioned);
        assertEquals(0, third.commissioned);
        assertEquals(0, third.decommissioned);
        assertEquals(1, second.decommissioned);
        assertEquals(2, first.decommissioned);
    }

    @Test
    public void firstCommissionFails() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new ErrorOnCommission(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first);
        WidgetLifecycle top = new WidgetLifecycle(tracker);
        Lifecycle<Widget> stack = createStack(others, top);
        try {
            stack.commission();
            fail("should have thrown commissionexception");
        } catch (ProgressiveLifecycleStackCommissionException e) {
            assertTrue(e.getCause() instanceof TestCommissionException);
        }
        assertEquals(0, first.commissioned);
        assertEquals(0, top.commissioned);
        assertEquals(0, first.decommissioned);
        assertEquals(0, top.decommissioned);
    }

    @Test
    public void topDecommissionFails() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new WidgetLifecycle(tracker), second = new WidgetLifecycle(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first, second);
        WidgetLifecycle third = new ErrorOnDecommission(tracker);
        Lifecycle<Widget> stack = createStack(others, third);
        stack.commission();
        try {
            stack.decommission();
        } catch (ProgressiveLifecycleStackDecommissionException e) {
            assertEquals(1, e.exceptionsThrown.size());
            Map.Entry<LifecycleStage<?, ?>, RuntimeException> entry = e.exceptionsThrown.entrySet().iterator().next();
            assertTrue(entry.getKey().isEquivalent(third));
            assertSame(TestRuntimeDecommissionException.class, entry.getValue().getClass());
        }
        assertEquals(1, first.commissioned);
        assertEquals(2, second.commissioned);
        assertEquals(3, third.commissioned);
        assertEquals(0, third.decommissioned);
        assertEquals(1, second.decommissioned);
        assertEquals(2, first.decommissioned);
    }

    private static class Widget {}

    private static class WidgetTracker {

        private final List<Widget> commissioned = (new ArrayList<>());
        private final AtomicInteger decommissioned = new AtomicInteger();

        public synchronized int registerCommissioned(Widget w) {
            commissioned.add(w);
            return commissioned.size();
        }

        public synchronized int registerDecommissioned() {
            return decommissioned.incrementAndGet();
        }
    }

    private static class WidgetLifecycle implements Lifecycle<Widget> {

        private final WidgetTracker tracker;
        public int commissioned, decommissioned;

        private WidgetLifecycle(WidgetTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public Widget commission() throws Exception {
            Widget w = new Widget();
            commissioned = tracker.registerCommissioned(w);
            return w;
        }
        @Override
        public void decommission() {
            decommissioned = tracker.registerDecommissioned();
        }

    }

    private static class ErrorOnCommission extends WidgetLifecycle {

        public ErrorOnCommission(WidgetTracker tracker) {
            super(tracker);
        }

        @Override
        public Widget commission() throws Exception {
            throw new TestCommissionException();
        }
    }

    private static final class TestCommissionException extends Exception {}

    private static final class ErrorOnDecommission extends WidgetLifecycle {

        public ErrorOnDecommission(WidgetTracker tracker) {
            super(tracker);
        }

        @Override
        public void decommission() {
            throw new TestRuntimeDecommissionException();
        }

    }

    private static final class TestRuntimeDecommissionException extends RuntimeException {}


    private static class IntStage implements Lifecycle<Integer> {

        private final List<Integer> collection;
        private Integer value;

        public IntStage(List<Integer> collection) {
            this.collection = collection;
        }

        @Override
        public Integer commission() {
            if (collection.isEmpty()) {
                value = 1;
            } else {
                value = collection.get(collection.size() - 1) + 1;
            }
            collection.add(value);
            return value;
        }

        @Override
        public void decommission() {
            collection.add(value);
        }
    }

    @Test
    public void testExecute_long() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = SimpleLifecycleStackBuilder.create()
                .addStage(new IntStage(sequence))
                .addStage(new IntStage(sequence))
                .addStage(new IntStage(sequence))
                .addStage(new IntStage(sequence))
                .finish(new IntStage(sequence));
        Integer commissioned = stack.commission();
        assertEquals("commissioned", 5, commissioned.intValue());
        assertEquals("sequence", Arrays.asList(1, 2, 3, 4, 5), sequence);
        stack.decommission();
        assertEquals("sequence", Arrays.asList(1, 2, 3, 4, 5, 5, 4, 3, 2, 1), sequence);
    }

    @Test
    public void testExecute_twoStages() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = SimpleLifecycleStackBuilder.create()
                .addStage(new IntStage(sequence))
                .finish(new IntStage(sequence));
        Integer commissioned = stack.commission();
        assertEquals("commissioned", 2, commissioned.intValue());
        assertEquals("sequence", Arrays.asList(1, 2), sequence);
        stack.decommission();
        assertEquals("sequence", Arrays.asList(1, 2, 2, 1), sequence);
    }

    @Test
    public void testExecute_one() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = SimpleLifecycleStackBuilder.create()
                .finish(new IntStage(sequence));
        Integer commissioned = stack.commission();
        assertEquals("commissioned", 1, commissioned.intValue());
        assertEquals("sequence", Arrays.asList(1), sequence);
        stack.decommission();
        assertEquals("sequence", Arrays.asList(1, 1), sequence);
    }

    private static class IntStageCommissionException extends RuntimeException {}

    @Test
    public void testExecute_interrupted() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = SimpleLifecycleStackBuilder.create()
                .addStage(new IntStage(sequence))
                .addStage(new IntStage(sequence))
                .addStage(new IntStage(sequence) {
                    @Override
                    public Integer commission() {
                        throw new IntStageCommissionException();
                    }
                })
                .addStage(new IntStage(sequence))
                .finish(new IntStage(sequence));
        try {
            stack.commission();
            fail("should have thrown LifecycleStackCommissionException");
        } catch (ProgressiveLifecycleStackCommissionException e) {
            assertTrue(e.getCause() instanceof IntStageCommissionException);
        }
        assertEquals("sequence", Arrays.asList(1, 2, 2, 1), sequence);

    }
}