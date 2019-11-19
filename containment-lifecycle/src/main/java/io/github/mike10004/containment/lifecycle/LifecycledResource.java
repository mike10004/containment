package io.github.mike10004.containment.lifecycle;

/**
 * Interface of a service that provides a resource with a lifecycle.
 * @param <T> resource type
 */
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

    /**
     * Returns a view of this resources as a scoped object, meaning
     * it can be used in a try-with-resources block.
     * Exiting the block will invoke {@link #finishLifecycle()}.
     * Calling this method will request  the resource.
     * @return a view of this resource as a scoped resource
     * @throws FirstProvisionFailedException if provisioning the resource fails
     */
    default ScopedResource<T> inScope() throws FirstProvisionFailedException {
        return new ScopedLifecycledResource<>(request().require(), this::finishLifecycle);
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
