package io.github.mike10004.containment.lifecycle;

/**
 * Exception superclass for exceptions thrown during the commission stage of a lifecycle stack.
 */
public class LifecycleStackCommissionException extends Exception {
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
