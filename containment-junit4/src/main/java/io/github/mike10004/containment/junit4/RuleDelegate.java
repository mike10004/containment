package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.RunningContainer;

public interface RuleDelegate {
    void before() throws Throwable;
    void after();
    RunningContainer container();
}
