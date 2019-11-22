package io.github.mike10004.containment.mavenplugin;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

class ConditionSetEvaluator {

    private static final Set<String> truthyValues = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("1", "yes", "true")));

    private final Predicate<? super String> parser = string -> {
        if (string == null) {
            return false;
        }
        string = string.trim().toLowerCase();
        return truthyValues.contains(string);
    };

    public ConditionSetEvaluator() {

    }

    private boolean isNullOrEmpty(@Nullable Object[] conditions) {
        return conditions == null || conditions.length == 0;
    }

    public boolean isAnyTrue(@Nullable String[] conditions) {
        if (isNullOrEmpty(conditions)) {
            return false;
        }
        return Arrays.stream(conditions).anyMatch(parser);
    }

    public boolean isAllTrue(@Nullable String[] conditions) {
        if (isNullOrEmpty(conditions)) {
            return false;
        }
        return Arrays.stream(conditions).allMatch(parser);
    }

}
