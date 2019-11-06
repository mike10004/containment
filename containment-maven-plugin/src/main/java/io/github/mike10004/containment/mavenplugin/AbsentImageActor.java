package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.exception.DockerException;
import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nullable;

/**
 * Interface of a service that performs an action
 * in reaction to the absence of an image.
 */
public interface AbsentImageActor {

    /**
     * Performs the action.
     * @param parametry parametry
     * @param directiveParameter directive
     * @throws MojoExecutionException on mojo error
     * @throws DockerException on docker error
     */
    void perform(RequireImageParametry parametry, @Nullable String directiveParameter) throws MojoExecutionException, DockerException;

}
