package io.github.mike10004.containment.lifecycle;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Interface of a value that represents the result of an attempt to
 * produce a resource.
 * @param <D> the resource type
 */
public interface Provision<D> {

    /**
     * Returns the value, or null if there was a failure to produce that value.
     * @return the value or null
     */
    @Nullable
    D value();

    /**
     * Returns an exception thrown during the attempt to produce the resource,
     * or null if no exception was thrown.
     * @return the exception thrown, or null
     */
    @Nullable
    Throwable exception();

    /**
     * Returns the resource or fails with an exception that describes the reason.
     * @return the resource, always non-null
     * @throws FirstProvisionFailedException if the attempt to produce the resource failed
     */
    default D require() throws FirstProvisionFailedException {
        if (!isSucceeded()) {
            throw new FirstProvisionFailedException(exception());
        }
        return value();
    }

    /**
     * Checks whether the attempt to produce the resource succeeded.
     * @return true iff the value is available
     */
    default boolean isSucceeded() {
        Throwable t = exception();
        return t == null;
    }

    /**
     * Returns an optional that represents this instance.
     * @return this instance as an optional
     */
    default Optional<D> asOptional() {
        return Optional.ofNullable(value());
    }

    /**
     * Returns a new instance representing a failure.
     * @param t  error
     * @param <D> value type
     * @return a new instance
     */
    static <D> Provision<D> failed(Throwable t) {
        return Computation.failed(t);
    }
}
