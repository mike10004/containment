package io.github.mike10004.containment;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class WellDefinedSocketAddress implements FullSocketAddress {

    private final String host;
    private final int port;

    public WellDefinedSocketAddress(String host, int port) {
        requireNonNull(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("0 < port <= 65535 is required");
        }
        if (host.trim().isEmpty()) {
            throw new IllegalArgumentException("host must be nonempty/nonwhitespace string");
        }
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getHost(), getPort());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WellDefinedSocketAddress)) return false;
        WellDefinedSocketAddress that = (WellDefinedSocketAddress) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
