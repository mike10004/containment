package io.github.mike10004.containment.lifecycle;

public interface DependencyLifecycle<D> {

    D commission() throws Exception;

    void decommission();

}
