package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.lifecycle.ContainerDependency;

class Globals {

    private Globals() {}

    public static ContainerParametry busyboxParametry() {
        return ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
    }

    public static final EventCollector GLOBAL_BUSYBOX_RULE_COLLECTOR = new EventCollector();

    public static final ContainerDependency GLOBAL_BUSYBOX_RULE = ContainerDependency
            .builder(busyboxParametry())
            .eventListener(GLOBAL_BUSYBOX_RULE_COLLECTOR)
            .buildGlobalDependency();
}
