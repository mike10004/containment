package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.lifecycle.ContainerResource;

class Globals {

    private Globals() {}

    public static ContainerParametry busyboxParametry() {
        return ContainerParametry.builder("busybox:latest")
                .commandToWaitIndefinitely()
                .build();
    }

    public static final EventCollector GLOBAL_BUSYBOX_RULE_COLLECTOR = new EventCollector();

    public static final ContainerResource GLOBAL_BUSYBOX_RULE = ContainerResource
            .builder(busyboxParametry())
            .eventListener(GLOBAL_BUSYBOX_RULE_COLLECTOR)
            .buildGlobalResource();
}
