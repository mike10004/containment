package io.github.mike10004.containment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ContainerParametry {

    public final String image;
    public final List<String> command;
    public final List<Integer> exposedPorts;
    public final Map<String, String> env;

    private ContainerParametry(Builder builder) {
        image = builder.image;
        command = builder.command;
        exposedPorts = new ArrayList<>(builder.exposedPorts);
        env = new LinkedHashMap<>(builder.env);
    }

    public static Builder builder(String image) {
        return new Builder(image);
    }

    public static final class Builder {

        private String image;

        private List<String> command = Collections.emptyList();

        private List<Integer> exposedPorts = new ArrayList<>();

        private Map<String, String> env = new LinkedHashMap<>();

        private Builder(String image) {
            this.image = requireNonNull(image);
        }

        public  Builder expose(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port value: " + port);
            }
            exposedPorts.add(port);
            return this;
        }

        public Builder command(List<String> val) {
            command = requireNonNull(val);
            return this;
        }

        public ContainerParametry build() {
            return new ContainerParametry(this);
        }

        public Builder env(String name, String value) {
            requireNonNull(name, "name");
            requireNonNull(value, "value");
            env.put(name, value);
            return this;
        }

    }
}
