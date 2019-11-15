package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;

/**
 * Exception superclass for exceptions thrown during the commission stage of a lifecycle stage stack.
 */
public class LifecycleStackCommissionException extends ContainmentException {

    public LifecycleStackCommissionException() {
    }

    public LifecycleStackCommissionException(String message) {
        super(message);
    }

    public LifecycleStackCommissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleStackCommissionException(Throwable cause) {
        super(cause);
    }

    public LifecycleStackCommissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
