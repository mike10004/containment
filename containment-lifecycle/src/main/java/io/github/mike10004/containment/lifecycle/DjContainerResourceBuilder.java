package io.github.mike10004.containment.lifecycle;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;
import io.github.mike10004.containment.dockerjava.DjShutdownHookContainerMonitor;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class DjContainerResourceBuilder extends ContainerResourceBuilder {

    private final Function<? super DjDockerManager, ? extends ContainerCreator> djContainerCreatorFactory;

    public DjContainerResourceBuilder(ContainerParametry containerParametry) {
        this(containerParametry, DjContainerCreator::new);
    }

    public DjContainerResourceBuilder(ContainerParametry containerParametry, Function<? super DjDockerManager, ? extends ContainerCreator> djContainerCreatorFactory) {
        super(containerParametry); // default DjContainerCreator::new
        this.djContainerCreatorFactory = requireNonNull(djContainerCreatorFactory);
    }


    /**
     * Constructor of creators for global containers. Cleanup of these containers is
     * managed by a {@link GlobalLifecyclingCachingProvider}, so they use
     * manual container monitors.
     */
    private class BuilderGlobalContainerCreatorConstructor extends GlobalContainerCreatorConstructor {

        public BuilderGlobalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
            super(djConstructor, DjContainerResourceBuilder.this::createManualContainerMonitor);
        }

    }

    /**
     * Constructor of creators for local containers. Cleanup of these containers is managed
     * by the caller, but a global monitor adds a shutdown hook to clean up ones the caller forgot.
     */
    private class BuilderLocalContainerCreatorConstructor extends LocalContainerCreatorConstructor {

        public BuilderLocalContainerCreatorConstructor(Function<? super DjDockerManager, ? extends ContainerCreator> djConstructor) {
            super(djConstructor, DjContainerResourceBuilder.this::createShutdownHookContainerMonitor);
        }
    }

    protected DjContainerMonitor createManualContainerMonitor(DockerClientConfig config) {
        return new DjManualContainerMonitor();
    }

    protected DjContainerMonitor createShutdownHookContainerMonitor(DockerClientConfig config) {
        return new DjShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(config).build());
    }

    @Override
    protected ContainerCreatorConstructor buildGlobalContainerCreatorConstructor() {
        return new BuilderGlobalContainerCreatorConstructor(djContainerCreatorFactory);
    }

    @Override
    protected ContainerCreatorConstructor buildLocalContainerCreatorConstructor() {
        return new BuilderLocalContainerCreatorConstructor(djContainerCreatorFactory);
    }
}
