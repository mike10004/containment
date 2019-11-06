package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import org.apache.maven.plugin.logging.Log;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

abstract class ClientAbsentImageActor extends AbsentImageActorBase {

    protected final Supplier<DockerClient> clientFactory;

    protected ClientAbsentImageActor(Log log, Supplier<DockerClient> clientFactory) {
        super(log);
        this.clientFactory = requireNonNull(clientFactory);
    }
}
