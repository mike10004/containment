package io.github.mike10004.containment.lifecycle;

public interface Lifecycle<D> {

    D commission() throws Exception;

    void decommission();

}
