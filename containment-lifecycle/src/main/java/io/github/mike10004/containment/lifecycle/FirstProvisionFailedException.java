package io.github.mike10004.containment.lifecycle;

/**
 * Exception that indicates that a provider failed to provide its target.
 */
public class FirstProvisionFailedException extends RuntimeException {

    /**
     * Constructs an instance.
     * @param cause cause of the failure
     */
    public FirstProvisionFailedException(Throwable cause) {
        super(cause);
    }

}
