package io.github.mike10004.containment;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Static utility methods dealing with durations.
 */
public class Durations {

    private static final CharMatcher NUMBERS = CharMatcher.inRange('0', '9');
    private static final CharMatcher LETTERS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));

    /**
     * Parses a duration specified in standard or human-friendly syntax.
     * @param definition the definition
     * @return a duration instance, or null if definition is null or empty
     * @see #parseDuration(String, Duration)
     */
    @Nullable
    public static Duration parseDuration(String definition) {
        return parseDuration(definition, null);
    }

    /**
     * Parses a duration specified in standard or human-friendly syntax.
     * Standard syntax is that which is accepted by {@link Duration#parse}.
     * If that method throws {@code DateTimeParseException}, then this method
     * attempts to parse using a human-friendly syntax.
     * Human-friendly syntax is of the form {@code Nu}, where {@code N} is an integer
     * and {@code u} is an optional unit, assumed to be milliseconds if absent.
     * The unit is parsed with {@link #parseUnit(String, TimeUnit)}.
     * @param definition the definition
     * @param defaultValue value to use if definition is null or empty
     * @return a duration instance
     */
    public static Duration parseDuration(String definition, Duration defaultValue) {
        if (definition != null && !definition.isEmpty()) {
            try {
                return Duration.parse(definition); // support standard duration syntax
            } catch (java.time.format.DateTimeParseException ignore) {
            }
            // then fall back to lax syntax
            definition = CharMatcher.whitespace().removeFrom(definition);
            String numbers = LETTERS.trimTrailingFrom(definition);
            String unitToken = NUMBERS.trimLeadingFrom(definition);
            TimeUnit unit = parseUnit(unitToken, TimeUnit.MILLISECONDS);
            long magnitude = Long.parseLong(numbers);
            long millis = unit.toMillis(magnitude);
            return Duration.ofMillis(millis);
        }
        return defaultValue;
    }

    /**
     * Parses a time unit.
     * Acceptable unit definitions are the following:
     * <ul>
     *     <li>ms</li>
     *     <li>millis</li>
     *     <li>milliseconds</li>
     *     <li>s</li>
     *     <li>sec</li>
     *     <li>secs</li>
     *     <li>seconds</li>
     *     <li>m</li>
     *     <li>min</li>
     *     <li>minutes</li>
     * </ul>
     * Any string parseable by {@link TimeUnit#valueOf(String)} is also acceptable.
     * @param unitToken the unit definition
     * @param defaultValue value if definition is null or empty
     * @return a time unit constant
     * @throws IllegalArgumentException if the token is not any acceptable value
     */
    public static TimeUnit parseUnit(String unitToken, TimeUnit defaultValue) {
        if (unitToken == null || unitToken.isEmpty()) {
            return defaultValue;
        }
        unitToken = unitToken.toLowerCase();
        switch (unitToken) {
            case "ms":
            case "milli":
            case "millis":
            case "milliseconds":
            case "millisecs":
            case "millisec":
                return TimeUnit.MILLISECONDS;
            case "s":
            case "sec":
            case "secs":
            case "seconds":
                return TimeUnit.SECONDS;
            case "m":
            case "min":
            case "mins":
            case "minutes":
                return TimeUnit.MINUTES;
        }
        try {
            return TimeUnit.valueOf(unitToken.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("failed to parse unit: " + StringUtils.abbreviate(unitToken, 128), e);
        }
    }

}
