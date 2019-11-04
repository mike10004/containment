package io.github.mike10004.containment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public interface ContainerParametry {

    ImageSpecifier image();

    List<String> command();

    List<Integer> exposedPorts();

    Map<String, String> environment();

    default boolean disableAutoRemove() {
        return false;
    }

    static Builder builder(String image) {
        return new Builder(image);
    }

    static Builder builder(ImageSpecifier image) {
        return new Builder(image);
    }

    final class Builder {

        private ImageSpecifier image;

        private List<String> command = Collections.emptyList();

        private List<Integer> exposedPorts = new ArrayList<>();

        private Map<String, String> env = new LinkedHashMap<>();

        private Builder(String image) {
            this(ImageSpecifier.parseSpecifier(requireNonNull(image, "image")));
        }

        private Builder(ImageSpecifier image) {
            this.image = requireNonNull(image);
        }

        public  Builder expose(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port value: " + port);
            }
            exposedPorts.add(port);
            return this;
        }

        public Builder commandToWaitIndefinitely() {
            return command(Arrays.asList("tail", "-f", "/dev/null"));
        }

        public Builder command(String first, String...others) {
            List<String> list = new ArrayList<>();
            list.add(first);
            list.addAll(Arrays.asList(others));
            return command(list);
        }

        public Builder command(List<String> val) {
            command = requireNonNull(val);
            return this;
        }

        public ContainerParametry build() {
            return new FrozenContainerParametry(this);
        }

        public Builder env(String name, String value) {
            requireNonNull(name, "name");
            requireNonNull(value, "value");
            env.put(name, value);
            return this;
        }

        private static class FrozenContainerParametry implements ContainerParametry {

            private final ImageSpecifier image;
            private final List<String> command;
            private final List<Integer> exposedPorts;
            private final Map<String, String> env;

            private FrozenContainerParametry(Builder builder) {
                image = builder.image;
                command = builder.command;
                exposedPorts = new ArrayList<>(builder.exposedPorts);
                env = new LinkedHashMap<>(builder.env);
            }

            public ImageSpecifier image() {
                return image;
            }

            public List<String> command() {
                return command;
            }

            public List<Integer> exposedPorts() {
                return exposedPorts;
            }

            public Map<String, String> environment() {
                return env;
            }

        }

    }
}
