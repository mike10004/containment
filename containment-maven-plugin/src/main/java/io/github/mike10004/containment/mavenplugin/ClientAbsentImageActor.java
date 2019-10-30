package io.github.mike10004.containment.mavenplugin;

import org.apache.maven.plugin.logging.Log;

import static java.util.Objects.requireNonNull;

abstract class ClientAbsentImageActor extends AbsentImageActorBase {

    protected final DockerManager dockerManager;

    protected ClientAbsentImageActor(Log log, DockerManager dockerManager) {
        super(log);
        this.dockerManager = requireNonNull(dockerManager);
    }
}
