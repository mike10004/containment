package io.github.mike10004.containment;

import javax.annotation.Nullable;
import java.util.Objects;

public class PortMapping {

    public final int containerPort;

    public final String containerProtocol;

    @Nullable
    public final FullSocketAddress host;

    public boolean isBound() {
        return host != null;
    }

    public PortMapping(int containerPort, String containerProtocol) {
        this(0, null, containerPort, containerProtocol);
    }

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
