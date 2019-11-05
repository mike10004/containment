package io.github.mike10004.containment.lifecycle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class LifecycleStackTest {

    @Test
    public void gardenPath() throws Exception {
        WidgetTracker tracker = new WidgetTracker();
        WidgetLifecycle first = new WidgetLifecycle(tracker), second = new WidgetLifecycle(tracker);
        List<WidgetLifecycle> others = Arrays.asList(first, second);
        WidgetLifecycle third = new WidgetLifecycle(tracker);
        LifecycleStack<Widget> stack = new LifecycleStack<>(others, third);
        Widget topWidget = stack.commission();
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
        LifecycleStack<Widget> stack = new LifecycleStack<>(others, third);
        try {
            stack.commission();
            fail("should have excepted");
        } catch (LifecycleStack.CommissionFailedAndUnwindFailedException e) {
            assertEquals(third, e.commissionExceptionThrower);
            assertTrue(e.commissionException.getClass() == CommissionException.class);
            assertEquals(1, e.unwindException.exceptionsThrown.size());
            Map.Entry<Lifecycle<?>, RuntimeException> entry = e.unwindException.exceptionsThrown.entrySet().iterator().next();
            assertEquals(entry.getKey(), second);
            assertEquals(entry.getValue().getClass(), RuntimeDecommissionException.class);
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
        LifecycleStack<Widget> stack = new LifecycleStack<>(others, third);
        try {
            stack.commission();
            fail("should have excepted");
        } catch (CommissionException ignore) {
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
        LifecycleStack<Widget> stack = new LifecycleStack<>(others, top);
        try {
            stack.commission();
            fail("should have thrown commissionexception");
        } catch (CommissionException ignore) {
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
        LifecycleStack<Widget> stack = new LifecycleStack<>(others, third);
        stack.commission();
        try {
            stack.decommission();
        } catch (LifecycleStack.UnwindException e) {
            assertEquals(1, e.exceptionsThrown.size());
            Map.Entry<Lifecycle<?>, RuntimeException> entry = e.exceptionsThrown.entrySet().iterator().next();
            assertSame(third, entry.getKey());
            assertSame(RuntimeDecommissionException.class, entry.getValue().getClass());
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
            throw new CommissionException();
        }
    }

    private static final class CommissionException extends Exception {}

    private static final class ErrorOnDecommission extends WidgetLifecycle {

        public ErrorOnDecommission(WidgetTracker tracker) {
            super(tracker);
        }

        @Override
        public void decommission() {
            throw new RuntimeDecommissionException();
        }

    }

    private static final class RuntimeDecommissionException extends RuntimeException {}

}