package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.exception.DockerException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;

class FailBuildActor extends AbsentImageActorBase {

    public FailBuildActor(Log mojoLog) {
        super(mojoLog);
    }

    @Override
    public void perform(RequireImageParametry parametry, @Nullable String directiveParameter) throws MojoExecutionException, DockerException {
        throw new RequiredImageAbsentException(String.format("failing build as directed because image '%s' is absent", parametry.name));
    }

    static class RequiredImageAbsentException extends MojoExecutionException {

        public RequiredImageAbsentException(String message) {
            super(message);
        }
    }

}
