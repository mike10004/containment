package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;

/**
 * Exception superclass for exceptions thrown during the commission stage of a lifecycle stage stack.
 */
public class ProgressiveLifecycleStackCommissionException extends ContainmentException {

    public ProgressiveLifecycleStackCommissionException() {
    }

    public ProgressiveLifecycleStackCommissionException(String message) {
        super(message);
    }

    public ProgressiveLifecycleStackCommissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProgressiveLifecycleStackCommissionException(Throwable cause) {
        super(cause);
    }

    public ProgressiveLifecycleStackCommissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
