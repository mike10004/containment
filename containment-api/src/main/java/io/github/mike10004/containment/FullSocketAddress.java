package io.github.mike10004.containment;

/**
 * Interface of a value class that represents a docket address.
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
     * @param host host
     * @param port port
     * @return new instance
     */
    static FullSocketAddress define(String host, int port) {
        return new WellDefinedSocketAddress(host, port);
    }

}

