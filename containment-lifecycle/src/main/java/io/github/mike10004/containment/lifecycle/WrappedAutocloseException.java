package io.github.mike10004.containment.lifecycle;

class WrappedAutocloseException extends RuntimeException {
    public WrappedAutocloseException(Exception cause) {
        super(cause);
    }
}
