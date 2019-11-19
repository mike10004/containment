package io.github.mike10004.containment;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public interface ContainerInspector {

    /**
     * Fetch ports of this container that may be bound to host ports.
     * @return a list of possibly-bound container ports
     * @throws ContainmentException if port list could not be fetched
     */
    List<ContainerPort> fetchPorts() throws ContainmentException;

    @Nullable
    default FullSocketAddress fetchHostPortBinding(int containerPort) throws ContainmentException {
        return fetchPorts().stream()
                .filter(c -> c.number() == containerPort)
                .map(ContainerPort::hostBinding)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

}
