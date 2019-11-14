package io.github.mike10004.containment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Interface of an object that defines parameters for creation and starting of a container.
 */
public interface ContainerParametry {

    /**
     * Returns the image specifier.
     * @return image specifier
     */
    ImageSpecifier image();

    /**
     * Returns the command to execute. An empty list is interpreted to mean the container's default
     * command is to be executed.
     * @return the command to execute
     */
    List<String> command();

    /**
     * Returns the command type.
     * @return command type
     */
    CommandType commandType();

    /**
     * Returns a list of ports within the container that are to be binded to ports on the container host.
     * @return list of ports
     */
    List<PortBinding> bindablePorts();

    List<BindMount> bindMounts();

    interface PortBinding {

        String toSerialForm();

        static PortBinding toHostFromContainer(int hostPort, int containerPort) {
            return () -> String.format("%d:%d", hostPort, containerPort);
        }

        static PortBinding containerOnly(int containerPort) {
            return () -> String.valueOf(containerPort);
        }
    }

    /**
     * Returns a map of environment variables available to the container.
     * @return map of environment variables
     */
    Map<String, String> environment();

    /**
     * Returns a flag that specifies whether automatic remove-on-stop should be disabled for
     * the container built from this parameter set.
     * @return flag specifying disabling of auto-remove
     */
    default boolean disableAutoRemoveOnStop() {
        return false;
    }

    /**
     * Creates a new builder for the image specified by the given string.
     * @param image image name and optional tag
     * @return a new builder instance
     * @see ImageSpecifier#parseSpecifier(String)
     */
    static Builder builder(String image) {
        return new Builder(image);
    }

    /**
     * Creates a new builder for the specified image.
     * @param image image specifier
     * @return a new builder instance
     */
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

    /**
     * Builder of parameter set instances.
     */
    final class Builder {

        private ImageSpecifier image;

        private CommandType commandType = CommandType.BLOCKING;

        private List<String> command = Collections.emptyList();

        private List<PortBinding> bindablePorts = new ArrayList<>();

        private List<BindMount> bindMounts = new ArrayList<>();

        private Map<String, String> env = new LinkedHashMap<>();

        private Builder(String image) {
            this(ImageSpecifier.parseSpecifier(requireNonNull(image, "image")));
        }

        private Builder(ImageSpecifier image) {
            this.image = requireNonNull(image);
        }

        public Builder mount(BindMount mount) {
            bindMounts.add(requireNonNull(mount));
            return this;
        }

        public Builder bindMount(File hostDirectory, String containerDirectory, BindMount.Permission permission) {
            return mount(new BindMount(hostDirectory.getAbsolutePath(), containerDirectory, permission));
        }

        public Builder bindMountReadOnly(File hostDirectory, String containerDirectory) {
            return bindMount(hostDirectory, containerDirectory, BindMount.Permission.READ_ONLY);
        }

        public Builder bindMountWriteAndRead(File hostDirectory, String containerDirectory) {
            return bindMount(hostDirectory, containerDirectory, BindMount.Permission.WRITE_AND_READ);
        }

        /**
         * Adds a container port to the list of bound ports. The port will be mapped to an unused port on the host.
         * @param containerPort the port
         * @return this builder instance
         * @see ContainerParametry#bindablePorts()
         */
        public Builder bindPort(int containerPort) {
            checkPort(containerPort);
            bindablePorts.add(PortBinding.containerOnly(containerPort));
            return this;
        }

        private static void checkPort(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid port value: " + port);
            }
        }

        /**
         * Adds a container port to the list of bound ports. The port will be mapped to an unused port on the host.
         * @param containerPort the port
         * @return this builder instance
         * @see ContainerParametry#bindablePorts()
         */
        public Builder bindPort(int containerPort, int hostPort) {
            checkPort(containerPort);
            checkPort(hostPort);
            bindablePorts.add(PortBinding.toHostFromContainer(hostPort, containerPort));
            return this;
        }

        /**
         * @deprecated use {@link #bindPort(int)}
         */
        @Deprecated
        public Builder bindablePort(int port) {
            return bindPort(port);
        }

        /**
         * Sets the command to one that will cause the container to remain alive indefinitely.
         * This implementation uses {@code tail -f /dev/null}.
         * @return this builder instance
         */
        public Builder commandToWaitIndefinitely() {
            // TODO decide whether `sleep infinity` is a better command to use
            return blockingCommand(Arrays.asList("tail", "-f", "/dev/null"));
        }

        /**
         * Sets the command to the given non-blocking command.
         * An empty list as the command argument is interpreted to mean that
         * the container's default command is to be used.
         * @param cmd the command
         * @return this builder instance
         * @see CommandType#EXITING_IMMEDIATELY
         */
        public Builder nonblockingCommand(List<String> cmd) {
            return command(CommandType.EXITING_IMMEDIATELY, cmd);
        }

        /**
         * Sets the command to the given non-blocking command.
         * @param first command executable
         * @param others command arguments
         * @return this builder instance
         * @see CommandType#EXITING_IMMEDIATELY
         */
        public Builder nonblockingCommand(String first, String...others) {
            return command(CommandType.EXITING_IMMEDIATELY, first, others);
        }

        /**
         * Sets the command to the given blocking command.
         * An empty list as the command argument is interpreted to mean that
         * the container's default command is to be used.
         * @param cmd the command
         * @return this builder instance
         * @see CommandType#BLOCKING
         */
        public Builder blockingCommand(List<String> cmd) {
            return command(CommandType.BLOCKING, cmd);
        }

        /**
         * Sets the command to the given blocking command.
         * @param first command executable
         * @param others command arguments
         * @return this builder instance
         * @see CommandType#BLOCKING
         */
        public Builder blockingCommand(String first, String...others) {
            return command(CommandType.BLOCKING, first, others);
        }

        /**
         * Sets the command type and command.
         * @param commandType command type
         * @param first command executable
         * @param others command arguments
         * @return this builder instance
         */
        public Builder command(CommandType commandType, String first, String...others) {
            List<String> list = new ArrayList<>();
            list.add(first);
            list.addAll(Arrays.asList(others));
            return command(commandType, list);
        }

        /**
         * Sets the command type and command.
         * An empty list as the command argument is interpreted to mean that
         * the container's default command is to be used.
         * @param commandType command type
         * @param command command
         * @return this builder instance
         */
        public Builder command(CommandType commandType, List<String> command) {
            this.command = requireNonNull(command);
            this.commandType = requireNonNull(commandType, "commandType");
            return this;
        }

        /**
         * Builds an immutable parameter set instance.
         * @return a new parameter set instance
         */
        public ContainerParametry build() {
            return new FrozenContainerParametry(this);
        }

        /**
         * Defines an environment variable to be added to the environment of the container
         * created from this parameter set.
         * @param name variable name
         * @param value variable value
         * @return this builder instance
         */
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
            private final List<PortBinding> exposedPorts;
            private final List<BindMount> bindMounts;
            private final Map<String, String> env;

            private FrozenContainerParametry(Builder builder) {
                image = builder.image;
                command = builder.command;
                exposedPorts = Collections.unmodifiableList(new ArrayList<>(builder.bindablePorts));
                env = Collections.unmodifiableMap(new LinkedHashMap<>(builder.env));
                this.commandType = requireNonNull(builder.commandType);
                bindMounts = Collections.unmodifiableList(new ArrayList<>(requireNonNull(builder.bindMounts)));
            }

            @Override
            public List<BindMount> bindMounts() {
                return bindMounts;
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
            public List<PortBinding> bindablePorts() {
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
                        .add("bindMounts=" + bindMounts)
                        .toString();
            }
        }

    }
}

