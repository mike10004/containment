package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.RunningContainer;
import io.github.mike10004.containment.lifecycle.ContainerLifecycles;
import io.github.mike10004.containment.lifecycle.Lifecycle;
import io.github.mike10004.containment.lifecycle.LifecycledResource;

class Globals {

    private Globals() {}

    public static final EventCollector GLOBAL_BUSYBOX_RULE_COLLECTOR;

    public static final LifecycledResource<RunningContainer> GLOBAL_BUSYBOX_RULE;

    static {
        GLOBAL_BUSYBOX_RULE_COLLECTOR = new EventCollector();
        ContainerParametry parametry = ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
        Lifecycle<RunningContainer> lifecycle = ContainerLifecycles.buildGlobal().creating(parametry).finish();
        GLOBAL_BUSYBOX_RULE = LifecycledResource
                .builder()
                .eventListener(GLOBAL_BUSYBOX_RULE_COLLECTOR)
                .buildGlobalResource(lifecycle);
    }
}
