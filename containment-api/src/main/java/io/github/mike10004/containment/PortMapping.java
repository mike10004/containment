package io.github.mike10004.containment;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Value class that represents a container port that may be bound.
 */
public class PortMapping {

    /**
     * Port number.
     */
    public final int containerPort;

    /**
     * Port protocol.
     */
    public final String containerProtocol;

    /**
     * Optional host binding. If this is non-null, then the container port is bound to a host port.
     */
    @Nullable
    public final FullSocketAddress host;

    /**
     * Returns true if this container port is bound to a host port.
     * @return
     */
    public boolean isBound() {
        return host != null;
    }

    /**
     * Constructs a new instance.
     * @param containerPort container port
     * @param containerProtocol port protocol
     */
    public static PortMapping unbound(int containerPort, String containerProtocol) {
        return new PortMapping(0, null, containerPort, containerProtocol);
    }

    /**
     * Constructs a new instance.
     * @param hostPort
     * @param hostAddress
     * @param containerPort
     * @param containerProtocol
     */
    public PortMapping(int hostPort, String hostAddress, int containerPort, String containerProtocol) {
        this.containerPort = containerPort;
        this.containerProtocol = containerProtocol;
        if (hostPort > 0) {
            host = new WellDefinedSocketAddress(hostAddress, hostPort);
        } else {
            host = null;
        }
    }

    @Override
    public String toString() {
        if (host != null) {
            return String.format("%s:%s->%s/%s", host.getHost(), host.getPort(), containerPort, containerProtocol);
        } else {
            return String.format("%s/%s", containerPort, containerProtocol);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortMapping)) return false;
        PortMapping that = (PortMapping) o;
        return containerPort == that.containerPort &&
                Objects.equals(containerProtocol, that.containerProtocol) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerPort, containerProtocol, host);
    }
}
