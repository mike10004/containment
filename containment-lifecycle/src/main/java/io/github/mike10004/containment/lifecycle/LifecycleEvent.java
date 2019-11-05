package io.github.mike10004.containment.lifecycle;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class LifecycleEvent {

    private final Category category;
    private final String message;

    public LifecycleEvent(Category category) {
        this(category, "");
    }

    public Category getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public LifecycleEvent(Category category, String message) {
        this.category = requireNonNull(category);
        this.message = Strings.nullToEmpty(message);
    }

    public static LifecycleEvent of(Category category) {
        requireNonNull(category, "category");
        return MESSAGELESS_EVENTS.get(category);
    }

    public enum Category {
        COMMISSION_STARTED,
        COMMISSION_SUCCEEDED,
        COMMISSION_FAILED,
        PROVIDE_STARTED,
        PROVIDE_COMPLETED,
        FINISH_STARTED,
        FINISH_COMPLETED,
        NOTICE
    }

    private static final Map<Category, LifecycleEvent> MESSAGELESS_EVENTS = Collections.unmodifiableMap(
            Arrays.stream(Category.values())
            .collect(Collectors.toMap(c -> c, LifecycleEvent::new)));

    @Override
    public final String toString() {
        StringJoiner j = new StringJoiner(", ", LifecycleEvent.class.getSimpleName() + "[", "]")
                .add("category=" + category);
        if (!message.isEmpty()) {
            j.add("message='" + message + "'");
        }
        return j.toString();
    }
}
