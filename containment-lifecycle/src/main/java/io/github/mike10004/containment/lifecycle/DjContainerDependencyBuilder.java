package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.dockerjava.DjDockerManager;

import java.util.function.Function;

public class DjContainerDependencyBuilder extends ContainerDependency.Builder {

    public DjContainerDependencyBuilder(ContainerParametry containerParametry, Function<? super DjDockerManager, ? extends ContainerCreator> djCreatorConstructor) {
        super(containerParametry, djCreatorConstructor);
    }

}
