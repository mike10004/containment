package io.github.mike10004.containment.lifecycle;

import static java.util.Objects.requireNonNull;

class ScopedLifecycledResource<T> implements ScopedResource<T> {

    private final Runnable closer;
    private final T required;

    public ScopedLifecycledResource(LifecycledResource<T> resource) throws FirstProvisionFailedException {
        this.required = requireNonNull(resource.request().require());
        closer = resource::finishLifecycle;
    }

    @Override
    public T acquire() {
        return required;
    }

    @Override
    public void close() throws RuntimeException {
        closer.run();
    }
}
