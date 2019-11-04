package io.github.mike10004.containment.lifecycle;

public interface Provision<D> {

    D value();

    Throwable exception();

    default D require() throws FirstProvisionFailedException {
        if (!isSucceeded()) {
            throw new FirstProvisionFailedException(exception());
        }
        return value();
    }

    default boolean isSucceeded() {
        Throwable t = exception();
        return t == null;
    }
}
