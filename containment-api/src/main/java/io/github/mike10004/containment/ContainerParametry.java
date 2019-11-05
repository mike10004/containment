package io.github.mike10004.containment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public interface ContainerParametry {

    ImageSpecifier image();

    List<String> command();

    CommandType commandType();

    List<Integer> exposedPorts();

    Map<String, String> environment();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean disableAutoRemove() {
        return false;
    }

    static Builder builder(String image) {
        return new Builder(image);
    }

    static Builder builder(ImageSpecifier image) {
        return new Builder(image);
    }

    /**
     * Enumeration of constants that represents types of commands executed
     * upon container start.
     */
    enum CommandType {

        /**
         * Command that blocks indefinitely. The container remains running.
         * This is the default assumed if no type is specified.
         */
        BLOCKING,

        /**
         * Command that exits immediately. The container stops after the command executes.
         */
        EXITING_IMMEDIATELY
    }

    final class Builder {

        private ImageSpecifier image;

        private CommandType commandType = CommandType.BLOCKING;

        private List<String> command = Collections.emptyList();

        private List<Integer> exposedPorts = new ArrayList<>();

        private Map<String, String> env = new LinkedHashMap<>();

        private Builder(String image) {
            this(ImageSpecifier.parseSpecifier(requireNonNull(image, "image")));
        }

        private Builder(ImageSpecifier image) {
            this.image = requireNonNull(image);
        }

        public Builder expose(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port value: " + port);
            }
            exposedPorts.add(port);
            return this;
        }

        public Builder commandToWaitIndefinitely() {
            return blockingCommand(Arrays.asList("tail", "-f", "/dev/null"));
        }

        public Builder nonblockingCommand(List<String> cmd) {
            return command(CommandType.EXITING_IMMEDIATELY, cmd);
        }

        public Builder nonblockingCommand(String first, String...others) {
            return command(CommandType.EXITING_IMMEDIATELY, first, others);
        }

        public Builder blockingCommand(List<String> cmd) {
            return command(CommandType.BLOCKING, cmd);
        }

        public Builder blockingCommand(String first, String...others) {
            return command(CommandType.BLOCKING, first, others);
        }

        public Builder command(CommandType commandType, String first, String...others) {
            List<String> list = new ArrayList<>();
            list.add(first);
            list.addAll(Arrays.asList(others));
            return command(commandType, list);
        }

        public Builder command(CommandType commandType, List<String> command) {
            this.command = requireNonNull(command);
            this.commandType = requireNonNull(commandType, "commandType");
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
            private final CommandType commandType;
            private final List<String> command;
            private final List<Integer> exposedPorts;
            private final Map<String, String> env;

            private FrozenContainerParametry(Builder builder) {
                image = builder.image;
                command = builder.command;
                exposedPorts = new ArrayList<>(builder.exposedPorts);
                env = new LinkedHashMap<>(builder.env);
                this.commandType = requireNonNull(builder.commandType);
            }

            @Override
            public CommandType commandType() {
                return commandType;
            }

            @Override
            public ImageSpecifier image() {
                return image;
            }

            @Override
            public List<String> command() {
                return command;
            }

            @Override
            public List<Integer> exposedPorts() {
                return exposedPorts;
            }

            @Override
            public Map<String, String> environment() {
                return env;
            }

            @Override
            public String toString() {
                return new StringJoiner(", ", "ContainerParametry" + "[", "]")
                        .add("image=" + image)
                        .add("command=" + command)
                        .add("exposedPorts=" + exposedPorts)
                        .add("env=" + env)
                        .toString();
            }
        }

    }
}

