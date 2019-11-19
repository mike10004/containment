package io.github.mike10004.containment.lifecycle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LifecycleStackTest {

    @Test
    public void builder_toSequence() {
        LifecycleStage<Void, TypeA> stage1 = stage(x -> new TypeA());
        LifecycleStage<TypeA, TypeB> stage2 = stage(x -> new TypeB());
        LifecycleStage<TypeB, TypeC> stage3 = stage(x -> new TypeC());
        Lifecycle<TypeC> lifecycle = LifecycleStack.startingAt(stage1)
                .andThen(stage2)
                .andThen(stage3)
                .toSequence();
        LifecycleStack<TypeC> stack = (LifecycleStack<TypeC>) lifecycle;
        assertEquals("stage sequence", Arrays.asList(stage1, stage2, stage3), stack.getStages());
    }

    private static final class TypeA {}
    private static final class TypeB {}
    private static final class TypeC {}

    private static <R, P> LifecycleStage<R, P> stage(Function<R, P> commissioner) {
        return stage(commissioner, ()->{});
    }

    private static <R, P> LifecycleStage<R, P> stage(Function<R, P> commissioner, Runnable decommissioner) {
        return new LifecycleStage<R, P>() {
            @Override
            public P commission(R requirement) throws Exception {
                return commissioner.apply(requirement);
            }

            @Override
            public void decommission() {
            }
        };
    }

    private static class IntStage implements LifecycleStage<Integer, Integer> {

        private final List<Integer> collection;
        private Integer value;

        public IntStage(List<Integer> collection) {
            this.collection = collection;
        }

        @Override
        public Integer commission(Integer requirement) {
            value = requirement.intValue() + 1;
            collection.add(value);
            return value;
        }

        @Override
        public void decommission() {
            collection.add(value);
        }
    }

    private static class FirstIntStage implements Lifecycle<Integer> {

        @Override
        public Integer commission() {
            return 0;
        }

        @Override
        public void decommission() {
        }
    }

    @Test
    public void testExecute() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = LifecycleStack.startingAt(new FirstIntStage())
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .toSequence();
        Integer commissioned = stack.commission();
        assertEquals("commissioned", 5, commissioned.intValue());
        assertEquals("sequence", Arrays.asList(1, 2, 3, 4, 5), sequence);
        stack.decommission();
        assertEquals("sequence", Arrays.asList(1, 2, 3, 4, 5, 5, 4, 3, 2, 1), sequence);
    }

    private static class IntStageCommissionException extends RuntimeException {}

    @Test
    public void testExecute_interrupted() throws Exception {
        List<Integer> sequence = Collections.synchronizedList(new ArrayList<>());
        Lifecycle<Integer> stack = LifecycleStack.startingAt(new FirstIntStage())
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence) {
                    @Override
                    public Integer commission(Integer requirement) {
                        throw new IntStageCommissionException();
                    }
                })
                .andThen(new IntStage(sequence))
                .andThen(new IntStage(sequence))
                .toSequence();
        try {
            stack.commission();
            fail("should have thrown LifecycleStageStackCommissionException");
        } catch (LifecycleStackCommissionException e) {
            assertTrue(e.getCause() instanceof IntStageCommissionException);
        }
        assertEquals("sequence", Arrays.asList(1, 2, 2, 1), sequence);

    }
}