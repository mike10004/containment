package io.github.mike10004.containment.lifecycle;

import static java.util.Objects.requireNonNull;

public interface ScopedResource<T> extends AutoCloseable {

    T acquire();

    @Override
    void close() throws RuntimeException;
}

