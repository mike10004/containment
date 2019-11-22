package io.github.mike10004.containment.mavenplugin;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConditionSetEvaluatorTest {

    @Test
    public void isAnyTrue1() {
        boolean actual = new ConditionSetEvaluator().isAnyTrue(new String[]{"true", "blah", null});
        assertTrue(actual);
    }

    @Test
    public void isAllTrue1() {
        boolean actual = new ConditionSetEvaluator().isAllTrue(new String[]{"true", "blah", null});
        assertFalse(actual);
    }

    @Test
    public void isAnyTrue2() {
        boolean actual = new ConditionSetEvaluator().isAnyTrue(new String[]{"nope", "blah", null});
        assertFalse(actual);
    }

    @Test
    public void isAllTrue2() {
        boolean actual = new ConditionSetEvaluator().isAllTrue(new String[]{"true", "yes", "1"});
        assertTrue(actual);
    }

    @Test
    public void isAnyTrue3() {
        boolean actual = new ConditionSetEvaluator().isAnyTrue(null);
        assertFalse(actual);
    }

    @Test
    public void isAllTrue3() {
        boolean actual = new ConditionSetEvaluator().isAllTrue(null);
        assertFalse(actual);
    }
}