package io.github.mike10004.containment.lifecycle;

public interface LifecycledResource<T> {

    /**
     * Requests a resource, commissioning it if not yet commissioned.
     * @return a resource provision
     */
    Provision<T> request();

    /**
     * Decommissions a resource if it has been commissioned. Does nothing
     * if resource has not been commissioned.
     */
    void finishLifecycle();

    default ScopedResource<T> inScope() {
        return new ScopedLifecycledResource<>(this);
    }

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
