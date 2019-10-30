package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PullResponseItem;
import com.google.common.base.Preconditions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

class PullImageActor extends ClientAbsentImageActor {

    public PullImageActor(Log mojoLog, DockerManager dockerManager) {
        super(mojoLog, dockerManager);
    }

    @Override
    public void perform(RequireImageParametry parametry, @Nullable String directiveParameter) throws MojoExecutionException, DockerException {
        String remoteImageName = resolveRemoteName(parametry, directiveParameter);
        DockerClient client = dockerManager.buildClient();
        ImageSpecifier remoteImageSpec = ImageSpecifier.parseSpecifier(remoteImageName).withDefaultTag("latest");
        PullImageCmd cmd = client.pullImageCmd(remoteImageSpec.toString());
        BlockableCallback<PullResponseItem> callback = new BlockableCallback<>();
        cmd.exec(callback);
        try {
            callback.completeOrThrowException(parametry.pullTimeout, () -> new MojoExecutionException("pull timeout exceeded"));
        } catch (InterruptedException e) {
            throw new MojoExecutionException("interrupted while waiting for pull to complete", e);
        }
        logger().info(String.format("pull %s completed", remoteImageName));
        boolean successful = callback.checkSucceeded(PullResponseItem::isPullSuccessIndicated);
        logger().info("image build complete; success: " + successful);
        if (!successful) {
            throw new MojoExecutionException("build completed unsuccessfully: " + callback.summarize());
        }
        if (!remoteImageName.equals(parametry.name)) {
            ImageSpecifier localImageSpec = ImageSpecifier.parseSpecifier(parametry.name);
            TagImageCmd tagCmd = client.tagImageCmd(remoteImageName, localImageSpec.toString(), localImageSpec.withDefaultTag("").tag);
            tagCmd.exec();
        }
    }

    protected String resolveRemoteName(RequireImageParametry parametry, @Nullable String directiveParameter) {
        requireNonNull(parametry, "parametry");
        Preconditions.checkArgument(!parametry.name.trim().isEmpty(), "image name must be nonempty");
        if (directiveParameter != null) {
            if (directiveParameter.trim().isEmpty()) {
                throw new IllegalArgumentException("parameter to pull directive must be either null or a string that specifies the repository image name, but this parameter is empty or all whitespace");
            }
            return directiveParameter;
        }
        return parametry.name;
    }
}
