package io.github.mike10004.containment.lifecycle;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Value class that represents a lifecycle event.
 */
public class LifecycleEvent {

    private final Category category;
    private final String message;

    /**
     * Constructs a new event instance.
     * @param category event category
     */
    public LifecycleEvent(Category category) {
        this(category, "");
    }

    /**
     * Gets the category.
     * @return category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Gets the message.
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Constructs an instance.
     * @param category category
     * @param message message
     */
    public LifecycleEvent(Category category, String message) {
        this.category = requireNonNull(category);
        this.message = Strings.nullToEmpty(message);
    }

    /**
     * Returns a message-less event with the given category.
     * @param category category
     * @return an event instance
     */
    public static LifecycleEvent of(Category category) {
        requireNonNull(category, "category");
        return MESSAGELESS_EVENTS.get(category);
    }

    /**
     * Enumeration of constants that represent categories of lifestyle events.
     */
    public enum Category {

        /**
         * Commission started.
         */
        COMMISSION_STARTED,

        /**
         * Commission finished successfully.
         */
        COMMISSION_SUCCEEDED,

        /**
         * Commission failed before finishing.
         */
        COMMISSION_FAILED,

        /**
         * Provision started.
         */
        PROVIDE_STARTED,

        /**
         * Provision completed.
         */
        PROVIDE_COMPLETED,

        /**
         * Finish procedure started.
         */
        FINISH_STARTED,

        /**
         * Finish procedure completed.
         */
        FINISH_COMPLETED,

        /**
         * Informational event.
         */
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

    private static final Consumer INACTIVE_CONSUMER = new Consumer() {
        @Override
        public void accept(Object o) {
            // no op
        }

        @Override
        public String toString() {
            return "Consumer{INACTIVE}";
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> inactiveConsumer() {
        return INACTIVE_CONSUMER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LifecycleEvent)) return false;
        LifecycleEvent that = (LifecycleEvent) o;
        return category == that.category &&
                message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, message);
    }
}
