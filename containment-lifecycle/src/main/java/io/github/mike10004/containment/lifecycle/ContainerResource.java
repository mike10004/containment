package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;

/**
 * Interface of a service that lazily provides a running container.
 */
public interface ContainerResource extends ProgressiveResource<StartedContainer> {

    /**
     * Creates an instance from a container provider.
     * @param containerProvider the provider
     * @return a new instance
     */
    static ContainerResource fromProvider(LifecyclingCachingProvider<StartedContainer> containerProvider) {
        return new CachableResource(containerProvider);
    }

    /**
     * Creates a new builder.
     * @param parametry container parameters
     * @return a new builder instance
     */
    static ContainerResourceBuilder builder(ContainerParametry parametry) {
        return new DjContainerResourceBuilder(parametry);
    }

}
