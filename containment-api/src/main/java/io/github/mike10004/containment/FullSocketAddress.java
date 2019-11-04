package io.github.mike10004.containment;

import com.google.common.net.HostAndPort;

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

    static FullSocketAddress define(String host, int port) {
        return new WellDefinedSocketAddress(host, port);
    }

    static FullSocketAddress fromHostAndPort(HostAndPort hostAndPort) {
        return new WellDefinedSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
    }

}

