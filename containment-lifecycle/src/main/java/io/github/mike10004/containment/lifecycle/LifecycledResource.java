package io.github.mike10004.containment.lifecycle;

public interface LifecycledResource<T> {

    /**
     * Returns an already-started container or starts and returns
     * a not-yet-started container.
     * @return a started container
     * @throws FirstProvisionFailedException if provisioning the container fails
     */
    T container() throws FirstProvisionFailedException;

    /**
     * Stops and removes the container, if it has been started.
     */
    void finishLifecycle();

    /**
     * Creates an instance from a container provider.
     * @param containerProvider the provider
     * @return a new instance
     */
    static <T> LifecycledResource<T> fromProvider(LifecyclingCachingProvider<T> containerProvider) {
        return new CacheableLifecycledResource<>(containerProvider);
    }

    static LifecycledResourceBuilder builder() {
        return new LifecycledResourceBuilder();
    }
}
