package io.github.mike10004.containment.lifecycle;

/**
 * Interface of a service that provides a resource and caches the resource.
 * In the case of a failure to produce the resource, the failure is cached
 * instead. A second attempt to produce a resource is never executed.
 * @param <T>
 */
public interface CachingProvider<T> {

    /**
     * Provides a provision that may contain the resource or a failure indication.
     * @return the provision
     */
    Provision<T> provide();

}

