package io.github.mike10004.containment.lifecycle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception class for errors that occur during the decommission stage of a lifecycle stack.
 */
public class LifecycleStackDecommissionException extends RuntimeException {

    /**
     * Map of exceptions thrown during the decommissioning.
     * Keys are the component lifecycles that threw the exceptions.
     */
    public final Map<Lifecycle<?>, RuntimeException> exceptionsThrown;

    /**
     * Constructs a new instance
     * @param exceptionsThrown exceptions
     */
    public LifecycleStackDecommissionException(Map<Lifecycle<?>, RuntimeException> exceptionsThrown) {
        super(String.format("%d lifecycle decommission methods threw exception(s): %s", exceptionsThrown.size(), exceptionsThrown.keySet()));
        this.exceptionsThrown = Collections.unmodifiableMap(new LinkedHashMap<>(exceptionsThrown));
    }
}
