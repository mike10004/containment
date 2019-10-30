package io.github.mike10004.containment.mavenplugin;

import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;

class IgnoreImageActor extends AbsentImageActorBase {

    public IgnoreImageActor(Log mojoLog) {
        super(mojoLog);
    }

    @Override
    public void perform(RequireImageParametry parametry, @Nullable String directiveParameter) {
        logger().info(String.format("ignoring absence of '%s' as directed", parametry.name));
    }

}
