package io.github.mike10004.containment;

public class ContainmentException extends Exception {

    public ContainmentException() {
    }

    public ContainmentException(String message) {
        super(message);
    }

    public ContainmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainmentException(Throwable cause) {
        super(cause);
    }
}
