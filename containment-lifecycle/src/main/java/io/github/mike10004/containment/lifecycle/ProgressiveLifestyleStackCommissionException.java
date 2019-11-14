package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainmentException;

/**
 * Exception superclass for exceptions thrown during the commission stage of a lifecycle stage stack.
 */
public class ProgressiveLifestyleStackCommissionException extends ContainmentException {

    public ProgressiveLifestyleStackCommissionException() {
    }

    public ProgressiveLifestyleStackCommissionException(String message) {
        super(message);
    }

    public ProgressiveLifestyleStackCommissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProgressiveLifestyleStackCommissionException(Throwable cause) {
        super(cause);
    }

    public ProgressiveLifestyleStackCommissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
