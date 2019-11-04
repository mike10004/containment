package io.github.mike10004.containment.lifecycle;

public interface LazyDependency<T> {

    Provision<T> provide();

}

