package io.github.mike10004.containment.lifecycle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProgressiveLifecyclesTest {

    private Random r;

    @Before
    public void setUp() {
        r = new Random("ContainerLifecyclesTest".hashCode());
    }

    private interface Validatable {
        boolean validate();
    }

    private static class TypeA implements  Validatable{
        @Override
        public boolean validate() {
            return true;
        }
    }

    private static class TypeB implements Validatable {
        public final TypeA a;

        private TypeB(TypeA a) {
            this.a = a;
        }
        @Override
        public boolean validate() {
            return a != null;
        }
    }

    private static class TypeC  implements Validatable {
        public final TypeB b;

        public TypeC(TypeB b) {
            this.b = b;
        }
        @Override
        public boolean validate() {
            return b != null;
        }
    }

    private static class TypeD  implements Validatable {
        public final TypeC c;

        public TypeD(TypeC c) {
            this.c = c;
        }
        @Override
        public boolean validate() {
            return c != null;
        }
    }

    private static final ContainerParametry EXAMPLE_PARAMETRY = ContainerParametry.builder("foo").build();

    private interface CheckValidate<X> {

        void assertValid(X thing);

        static <V extends Validatable> CheckValidate<V> of(Class<V> expectedClass) {
            return new CheckValidate<V>() {
                @Override
                public void assertValid(V produced) {
                    assertTrue("validate()", produced.validate());
                    assertTrue(String.format("%s instanceof %s", produced, expectedClass), expectedClass.isInstance(produced));
                }
            };
        }
    }

    private void checkStack(LifecycleStageStack<StartedContainer> stack, UnitTestContainerMonitor monitor) throws Exception {
        checkStack(stack, new CheckValidate<StartedContainer>(){
            @Override
            public void assertValid(StartedContainer thing) {
                assertNotNull(thing);
            }
        }, monitor);
    }

    private <X extends Validatable> void checkStack(LifecycleStageStack<X> stack, Class<X> expectedClass, UnitTestContainerMonitor monitor) throws Exception {
        checkStack(stack, CheckValidate.of(expectedClass), monitor);
    }

    private <X> void checkStack(LifecycleStageStack<X> stack, CheckValidate<X> validator, UnitTestContainerMonitor monitor) throws Exception {
        assertNotNull("stack", stack);
        X produced = stack.commission();
        validator.assertValid(produced);
        assertEquals("containers seen by monitor", 1, monitor.actions.asMap().keySet().size());
        assertEquals(Arrays.asList(MonitoredAction.created, MonitoredAction.started), new ArrayList<>(monitor.actions.values()));
        stack.decommission();
        assertEquals("containers seen by monitor", 1, monitor.actions.asMap().keySet().size());
        assertEquals(Arrays.asList(MonitoredAction.values()), new ArrayList<>(monitor.actions.values()));
    }

    @Test
    public void example_1_full() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<TypeD> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .pre(container -> new TypeA())
                .pre((container, a) -> new TypeB(a))
                .post((container, b) -> new TypeC(b))
                .post((container, c) -> new TypeD(c))
                .finish();
        checkStack(stack, TypeD.class, m);
    }

    @Test
    public void example_2() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<TypeC> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .pre(container -> new TypeA())
                .pre((container, a) -> new TypeB(a))
                .post((container, b) -> new TypeC(b))
                .finish();
        checkStack(stack, TypeC.class, m);
    }

    private enum MonitoredAction { created, started, stopped, removed }

    private static class UnitTestContainerMonitor implements DjContainerMonitor {

        public final Multimap<String, MonitoredAction> actions = ArrayListMultimap.create();

        @Override
        public void created(String containerId) {
            actions.put(containerId, MonitoredAction.created);
        }

        @Override
        public void started(String containerId) {
            actions.put(containerId, MonitoredAction.started);
        }

        @Override
        public void stopped(String containerId) {
            actions.put(containerId, MonitoredAction.stopped);
        }

        @Override
        public void removed(String containerId) {
            actions.put(containerId, MonitoredAction.removed);
        }
    }

    @Test
    public void example_3() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<TypeB> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .pre(container -> new TypeA())
                .pre((container, a) -> new TypeB(a))
                .finish();
        checkStack(stack, TypeB.class, m);
    }

    @Test
    public void example_4() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<TypeA> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .pre(container -> new TypeA())
                .finish();
        checkStack(stack, TypeA.class, m);
    }

    @Test
    public void example_6() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<TypeA> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .post(container -> new TypeA())
                .finish();
        checkStack(stack, TypeA.class, m);
    }

    @Test
    public void example_5() throws Exception {
        UnitTestContainerMonitor m = new UnitTestContainerMonitor();
        LifecycleStageStack<StartedContainer> stack = ProgressiveLifecycles.builder(() -> new UnitTestContainerCreator(m, r))
                .startedWith(EXAMPLE_PARAMETRY)
                .finish();
        checkStack(stack, m);
    }

}