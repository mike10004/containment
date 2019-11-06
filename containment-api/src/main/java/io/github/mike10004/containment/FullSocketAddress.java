package io.github.mike10004.containment;

/**
 * Interface of a value class that represents a well-defined socket address.
 * A well defined socket address includes both the host, as a domain name or
 * IP address, and the port number.
 * This differs from, say, Guava's {@code HostAndPort}, in that a valid port
 * number is required.
 */
public interface FullSocketAddress {

    /**
     * Gets the hostname or IP address.
     * @return host
     */
    String getHost();

    /**
     * Gets the port.
     * @return port
     */
    int getPort();

    /**
     * Creates a new instance.
     * This default implementation does limited checking of the validity of
     * the host string, so invalid hosts such as 0.0.0.0 may be specified.
     *
     * @param host host; must be non-null, non-empty, and non-whitespace
     * @param port port; must be between 1 and 65535
     * @return new instance
     */
    static FullSocketAddress define(String host, int port) {
        return new WellDefinedSocketAddress(host, port);
    }

}

