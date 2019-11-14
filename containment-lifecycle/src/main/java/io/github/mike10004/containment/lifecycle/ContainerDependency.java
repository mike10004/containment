package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.StartedContainer;

/**
 * Interface of a service that lazily provides a running container.
 */
public interface ContainerDependency extends ProgressiveDependency<StartedContainer> {

    /**
     * Creates an instance from a container provider.
     * @param containerProvider the provider
     * @return a new instance
     */
    static ContainerDependency fromProvider(LifecyclingCachingProvider<StartedContainer> containerProvider) {
        return new CachableDependency(containerProvider);
    }

    /**
     * Creates a new builder.
     * @param parametry container parameters
     * @return a new builder instance
     */
    static ContainerDependencyBuilder builder(ContainerParametry parametry) {
        return new ContainerDependencyBuilder(parametry);
    }

}
