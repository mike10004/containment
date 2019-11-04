package io.github.mike10004.containment.mavenplugin;

import io.github.mike10004.containment.dockerjava.DjDockerManager;
import org.apache.maven.plugin.logging.Log;

import static java.util.Objects.requireNonNull;

abstract class ClientAbsentImageActor extends AbsentImageActorBase {

    protected final DjDockerManager dockerManager;

    protected ClientAbsentImageActor(Log log, DjDockerManager dockerManager) {
        super(log);
        this.dockerManager = requireNonNull(dockerManager);
    }
}
