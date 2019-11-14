package io.github.mike10004.containment;

/**
 * Exception superclass for errors that occur incident to actions relating to containers.
 */
public class ContainmentException extends Exception {

    /**
     * {@inheritDoc}
     */
    public ContainmentException() {
    }

    /**
     * {@inheritDoc}
     */
    public ContainmentException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public ContainmentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public ContainmentException(Throwable cause) {
        super(cause);
    }

    protected ContainmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
