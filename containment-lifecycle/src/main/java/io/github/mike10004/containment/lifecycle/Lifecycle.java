package io.github.mike10004.containment.lifecycle;

/**
 *
 * @param <D>
 */
public interface Lifecycle<D> {

    D commission() throws Exception;

    void decommission();

}
