package io.github.mike10004.containment.lifecycle;

public interface ProgressiveResource<T> {

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
    static <T> ProgressiveResource<T> fromProvider(LifecyclingCachingProvider<T> containerProvider) {
        return new CacheableProgressiveResource<>(containerProvider);
    }

    static ProgressiveResourceBuilder builder() {
        return new ProgressiveResourceBuilder();
    }
}
