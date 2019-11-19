package io.github.mike10004.containment.lifecycle;

import static java.util.Objects.requireNonNull;

class ScopedLifecycledResource<T> implements ScopedResource<T> {

    private final Runnable closer;
    private final T required;

    public ScopedLifecycledResource(T resource, Runnable closer) throws FirstProvisionFailedException {
        this.required = requireNonNull(resource);
        this.closer = requireNonNull(closer);
    }

    /**
     * Gets the resource. This does not throw an exception; the resource has already been required.
     * @return the resource
     */
    @Override
    public T acquire() {
        return required;
    }

    @Override
    public void close() throws RuntimeException {
        closer.run();
    }

}
