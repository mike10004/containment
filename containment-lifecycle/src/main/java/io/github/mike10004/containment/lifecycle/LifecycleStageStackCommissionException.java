package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;

/**
 * Exception superclass for exceptions thrown during the commission stage of a lifecycle stage stack.
 */
public class LifecycleStageStackCommissionException extends ContainmentException {

    public LifecycleStageStackCommissionException() {
    }

    public LifecycleStageStackCommissionException(String message) {
        super(message);
    }

    public LifecycleStageStackCommissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleStageStackCommissionException(Throwable cause) {
        super(cause);
    }

    public LifecycleStageStackCommissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
