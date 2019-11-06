package io.github.mike10004.containment;

import javax.annotation.Nullable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Value class that represents a container port that may be bound.
 */
public class ContainerPort {

    private final int number;

    private final String protocol;

    @Nullable
    private final FullSocketAddress host;

    /**
     * Returns true if this container port is bound to a host port.
     * @return true if bound
     */
    public boolean isBound() {
        return hostBinding() != null;
    }

    /**
     * Constructs a new instance.
     * @param containerPort container port
     * @param containerProtocol port protocol
     */
    public static ContainerPort unbound(int containerPort, String containerProtocol) {
        return new ContainerPort(0, null, containerPort, containerProtocol);
    }

    /**
     * Constructs a new instance.
     * @param host host address; must be non-null
     * @param number container port number
     * @param protocol container port protocol
     */
    public static ContainerPort bound(int number, String protocol, FullSocketAddress host) {
        return new ContainerPort(number, protocol, requireNonNull(host, "host"));
    }

    private ContainerPort(int number, String protocol, @Nullable FullSocketAddress host) {
        this.number = number;
        this.protocol = requireNonNull(protocol, "protocol");
        this.host = host;
    }

    /**
     * Constructs a new instance.
     * @param hostPort container host port number
     * @param hostAddress container host host
     * @param number container port number
     * @param protocol container port protocol
     */
    @Deprecated
    private ContainerPort(int hostPort, String hostAddress, int number, String protocol) {
        this(number, protocol, hostPort > 0 ? FullSocketAddress.define(hostAddress, hostPort) : null);
    }

    @Override
    public String toString() {
        FullSocketAddress host = hostBinding();
        if (host != null) {
            return String.format("%s:%s->%s/%s", host.getHost(), host.getPort(), number(), protocol());
        } else {
            return String.format("%s/%s", number(), protocol());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContainerPort)) return false;
        ContainerPort that = (ContainerPort) o;
        return number == that.number &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, protocol, host);
    }

    /**
     * Port number.
     */
    public int number() {
        return number;
    }

    /**
     * Port protocol.
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Optional host binding. If this is non-null, then the container port is bound to a host port.
     */
    @Nullable
    public FullSocketAddress hostBinding() {
        return host;
    }
}
