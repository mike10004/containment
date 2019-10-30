package io.github.mike10004.containment.mavenplugin;

import org.apache.maven.plugin.logging.Log;

import static java.util.Objects.requireNonNull;

abstract class AbsentImageActorBase implements AbsentImageActor {

    private final Log mojoLog;

    public AbsentImageActorBase(Log mojoLog) {
        this.mojoLog = requireNonNull(mojoLog);
    }

    protected Log logger() {
        return mojoLog;
    }

}
