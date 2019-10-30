package io.github.mike10004.containment.mavenplugin;

import com.google.common.collect.Maps;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Value class that represents options the mojo is configured with.
 * Fields of this class exclude parameters that would be specified
 * by a parameter to a {@link AbsentImageDirective directive}.
 */
public class RequireImageParametry {

    /**
     * Name plus optional tag in format {@code name[:tag]}.
     */
    public final String name;

    public final Duration pullTimeout;

    public final Properties buildArgs;

    public final Duration buildTimeout;

    public final Map<String, String> labels;

    private RequireImageParametry(Builder builder) {
        name = requireNonNull(builder.name);
        pullTimeout = requireNonNull(builder.pullTimeout);
        buildTimeout = requireNonNull(builder.buildTimeout);
        buildArgs = requireNonNull(builder.buildArgs);
        labels = new LinkedHashMap<>(requireNonNull(builder.labels));
    }

    public static Builder newBuilder(String name) {
        return new Builder().name(name);
    }

    static final Duration DEFAULT_BUILD_TIMEOUT = Duration.ofMinutes(30);
    static final Duration DEFAULT_PULL_TIMEOUT = Duration.ofMinutes(30);

    public static final class Builder {

        private String name;
        private Duration pullTimeout = DEFAULT_PULL_TIMEOUT;
        private final Properties buildArgs = new Properties();
        private Duration buildTimeout = DEFAULT_BUILD_TIMEOUT;
        private Map<String, String> labels = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder name(String val) {
            name = val;
            return this;
        }

        public Builder pullTimeout(Duration val) {
            pullTimeout = val;
            return this;
        }

        public RequireImageParametry build() {
            return new RequireImageParametry(this);
        }

        public Builder buildArgs(Properties additive) {
            additive.stringPropertyNames().forEach(n -> buildArg(n, additive.getProperty(n)));
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder buildArg(String name, String value) {
            buildArgs.setProperty(name, value);
            return this;
        }

        public Builder buildTimeout(Duration duration) {
            this.buildTimeout = duration;
            return this;
        }

        public Builder label(String name, String value) {
            labels.put(name, value);
            return this;
        }

        public Builder labels(Properties additive) {
            labels.putAll(Maps.fromProperties(additive));
            return this;
        }
    }
}
