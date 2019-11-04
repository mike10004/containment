package io.github.mike10004.containment;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DurationsTest {

    @Test
    public void parseDuration() {
        TestCase[] testCases = {
                TestCase.millis("123ms", 123),
                TestCase.millis("1 millisecond", 1),
                TestCase.millis("123 ms", 123),
                TestCase.millis("123 milliseconds", 123),
                new TestCase("1min", Duration.ofMinutes(1)),
                new TestCase("1 min", Duration.ofMinutes(1)),
                new TestCase("1m", Duration.ofMinutes(1)),
                new TestCase("1 minute", Duration.ofMinutes(1)),
                TestCase.seconds("456sec", 456),
                TestCase.seconds("456s", 456),
                TestCase.seconds("1 second", 1),
                TestCase.seconds("456seconds", 456),
                TestCase.seconds("456 sec", 456),
                TestCase.seconds("456 s", 456),
                TestCase.seconds("456 seconds", 456),
                TestCase.formal(Duration.ofMillis(789)),
                TestCase.formal(Duration.ofSeconds(30)),
                TestCase.formal(Duration.ofSeconds(300)),
                TestCase.formal(Duration.ofHours(36)),
                TestCase.formal(Duration.ofHours(12)),
                TestCase.formal(Duration.ofDays(3)),
                TestCase.formal(Duration.ofDays(30)),
                TestCase.formal(Duration.ofDays(365)),
                TestCase.formal(Duration.ofDays(400)),
        };
        for (TestCase testCase : testCases) {
            assertEquals(testCase.toString(), testCase.expected, Durations.parseDuration(testCase.input));
        }
    }

    @Test
    public void parseDuration_shouldCauseError() {
        String[] errorCausers = {
                "P",      //
                "P6M3H",  // hours, minutes in wrong order
                "t15M",   // bad case
                "1 hour",
                "1 h",
                "1h",
                "3d",
                "minute",
                "sec",
        };
        Map<String, Duration> unexpected = new LinkedHashMap<>();
        for (String testCase : errorCausers) {
            try {
                unexpected.put(testCase, Durations.parseDuration(testCase));
            } catch (RuntimeException ignore) {
            }
        }
        assertEquals("expect zero parseable values", Collections.<String, Duration>emptyMap(), unexpected);
    }

    private static class TestCase {
        public final String input;
        public final Duration expected;

        public TestCase(String input, Duration expected) {
            this.input = input;
            this.expected = expected;
        }

        public static TestCase millis(String input, long millis) {
            return new TestCase(input, Duration.ofMillis(millis));
        }

        public static TestCase seconds(String input, long sec) {
            return new TestCase(input, Duration.ofSeconds(sec));
        }

        public static TestCase formal(Duration duration) {
            return new TestCase(duration.toString(), duration);
        }

        public String toString() {
            return String.format("TestCase{input=\"%s\",expected=%s}", StringEscapeUtils.escapeJava(input), expected);
        }
    }

    @Test
    public void parseUnit() {
        List<Triple<String, TimeUnit, TimeUnit>> testCases = new ArrayList<>();
        testCases.add(Triple.of("", null, null));
        testCases.add(Triple.of(null, null, null));
        testCases.add(Triple.of("ms", null, TimeUnit.MILLISECONDS));
        testCases.add(Triple.of("millis", null, TimeUnit.MILLISECONDS));
        testCases.add(Triple.of("milliseconds", null, TimeUnit.MILLISECONDS));
        testCases.add(Triple.of("s", null, TimeUnit.SECONDS));
        testCases.add(Triple.of("sec", null, TimeUnit.SECONDS));
        testCases.add(Triple.of("secs", null, TimeUnit.SECONDS));
        testCases.add(Triple.of("seconds", null, TimeUnit.SECONDS));
        testCases.add(Triple.of("m", null, TimeUnit.MINUTES));
        testCases.add(Triple.of("min", null, TimeUnit.MINUTES));
        testCases.add(Triple.of("minutes", null, TimeUnit.MINUTES));
        List<Object> failures = new ArrayList<>();
        for (Triple<String, TimeUnit, TimeUnit> testCase : testCases) {
            String token = testCase.getLeft();
            TimeUnit df = testCase.getMiddle();
            TimeUnit expected = testCase.getRight();
            TimeUnit actual = Durations.parseUnit(token, df);
            if (!Objects.equals(expected, actual)) {
                failures.add(Arrays.asList(token, df, expected, actual));
            }
        }
        assertEquals("failures", Collections.emptyList(), failures);
    }
}