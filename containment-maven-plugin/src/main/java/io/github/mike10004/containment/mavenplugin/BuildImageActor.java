package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.BuildResponseItem;
import io.github.mike10004.containment.dockerjava.BlockableCallback;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

class BuildImageActor extends ClientAbsentImageActor {

    private final Function<String, String> mavenPropertiesProvider;

    public BuildImageActor(Log log, DjDockerManager dockerManager, Function<String, String> mavenPropertiesProvider) {
        super(log, dockerManager);
        this.mavenPropertiesProvider = requireNonNull(mavenPropertiesProvider);
    }

    protected BuildImageCmd createCommand(RequireImageParametry parametry, File dockerfileDir) {
        DockerClient client = dockerManager.openClient();
        BuildImageCmd buildCmd = client.buildImageCmd(dockerfileDir);
        buildCmd.withTags(Collections.singleton(parametry.name));
        buildCmd.withLabels(parametry.labels);
        parametry.buildArgs.forEach(buildCmd::withBuildArg);
        return buildCmd;
    }

    @Override
    public void perform(RequireImageParametry parametry, @Nullable String directiveParameter) throws MojoExecutionException, DockerException {
        File dockerfileDir;
        if (directiveParameter == null) {
            dockerfileDir = resolveDefaultDockerfileDir();
        } else {
            dockerfileDir = new File(directiveParameter);
        }
        BuildImageCmd buildCmd = createCommand(parametry, dockerfileDir);
        BlockableCallback<BuildResponseItem> callback = BlockableCallback.createSuccessCheckingCallback(BuildResponseItem::isBuildSuccessIndicated);
        logger().info(String.format("starting build of %s using path %s", parametry.name, dockerfileDir));
        buildCmd.exec(callback);
        try {
            callback.completeOrThrowException(parametry.buildTimeout, () -> new MojoExecutionException("build timeout exceeded"));
        } catch (InterruptedException e) {
            throw new MojoExecutionException("interrupted while waiting for build to complete", e);
        }
        boolean successful = callback.checkSucceeded();
        logger().info("image build complete; success: " + successful);
        if (!successful) {
            throw new MojoExecutionException("build completed unsuccessfully: " + callback.summarize());
        }
    }

    private static class InvalidDockerfileDirException extends MojoExecutionException {

        public InvalidDockerfileDirException(String message) {
            super(message);
        }
    }

    private void checkDockerfileDirOk(File dockerfileDir) throws MojoExecutionException {
        if (!dockerfileDir.isDirectory()) {
            throw new InvalidDockerfileDirException("not a directory: " + dockerfileDir);
        }
        File dockerfile = new File(dockerfileDir, "Dockerfile");
        if (!dockerfile.isFile()) {
            try {
                dockerfile = java.nio.file.Files.walk(dockerfileDir.toPath(), 0)
                        .map(Path::toFile)
                        .filter(f -> f.isFile() && "dockerfile".equalsIgnoreCase(f.getName()))
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                throw new MojoExecutionException("failed to find dockerfile", e);
            }
        }
        if (dockerfile == null) {
            throw new InvalidDockerfileDirException("no dockerfile in directory " + dockerfileDir);
        }
        if (!dockerfile.canRead()) {
            throw new InvalidDockerfileDirException("dockerfile not readable");
        }
    }

    protected File resolveDefaultDockerfileDir() throws MojoExecutionException {
        String baseDir = mavenPropertiesProvider.apply("project.basedir");
        checkState(baseDir != null && !baseDir.isEmpty() && !"${project.basedir}".equals(baseDir), "maven properties are not available");
        File baseDirectory = new File(baseDir);
        File dockerfileDir = baseDirectory.toPath().resolve("src").resolve("test").resolve("docker").toFile();
        checkDockerfileDirOk(dockerfileDir);
        return dockerfileDir;
    }
}
