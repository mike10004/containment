package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.nitsick.Durations;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Goal which enforces the local existence of a container image.
 */
@Mojo( name = RequireImageMojo.GOAL, defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES )
public class RequireImageMojo extends AbstractMojo {

    static final String GOAL = "require-image";

    /**
     * Name and optionally a tag in the 'name[:tag]' format.
     */
    @Parameter( required = true )
    private String imageName;

    /**
     * Action to perform if the image is not present locally. Valid values are
     * <ul>
     *     <li>{@code fail} to fail the Maven build</li>
     *     <li>{@code ignore} to allow the Maven build to continue</li>
     *     <li>{@code build[:/path/to/directoryWithDockerfile]} to build the image from the dockerfile
     *         contained in the directory whose pathname is specified; if no pathname is specified,
     *         then <code>${project.basedir}/src/test/docker</code> is assumed</li>
     *     <li>{@code pull[:IMAGE_NAME]} to pull the image whose name is specified,
     *     where {@code IMAGE_NAME} is in {@code name[:tag]} format; if no image name is specified,
     *     then the image specified by the {@code name} parameter is used</li>
     * </ul>
     */
    @Parameter(defaultValue = "fail")
    private String absentImageAction;

    /**
     * Maven project. Injected automatically.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Build args. Use markup like the following:
     * <pre>
     *     &lt;buildArgs&t;
     *       &lt;name1&gt;value1&lt;/name1&gt;
     *       &lt;name2&gt;value2&lt;/name2&gt;
     *     &lt;/buildArgs&t;
     * </pre>
     */
    @Parameter
    private Map<String, String> buildArgs;

    /**
     * Labels to apply to a freshly-built image.
     *
     * Use markup like the following:
     * <pre>
     *     &lt;imageLabels&t;
     *       &lt;name1&gt;value1&lt;/name1&gt;
     *       &lt;name2&gt;value2&lt;/name2&gt;
     *     &lt;/imageLabels&t;
     * </pre>
     */
    @Parameter
    private Map<String, String> buildLabels;

    /**
     * Timeout for an image build operation, if one is necessary.
     * Syntax is human readable, but integer numbers are required.
     * Values such as {@code 5min}, {@code 30 seconds}, or {@code 90000ms} are allowed.
     */
    @Parameter
    private String buildTimeout;

    /**
     * Timeout for an image pull operation, if one is necessary.
     * Syntax is human readable, but integer numbers are required.
     * Values such as {@code 5min}, {@code 30 seconds}, or {@code 90000ms} are allowed.
     */
    @Parameter
    private String pullTimeout;

    public RequireImageMojo() {

    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getAbsentImageAction() {
        return absentImageAction;
    }

    void setAbsentImageAction(AbsentImageAction action, @Nullable String parameter) {
        requireNonNull(action, "action");
        String value = parameter == null ? action.name() : String.format("%s%s%s", action.name(), AbsentImageDirective.DELIMITER, parameter);
        setAbsentImageAction(value);
    }

    public void setAbsentImageAction(String absentImageAction) {
        this.absentImageAction = absentImageAction;
    }

    protected Function<String, String> createMavenPropertiesProvider() {
        return propertyName -> {
            if (project != null) {
                return project.getProperties().getProperty(propertyName);
            }
            return null;
        };
    }

    private static DockerClientConfig createConfig(MavenProject project, RequireImageParametry parametry) {
        // TODO parameterize config builder from maven project and require-image goal configuration
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    public void execute() throws MojoExecutionException {
        requireNonNull(absentImageAction, "absentImageAction");
        AbsentImageDirective directive = AbsentImageDirective.parse(absentImageAction);
        RequireImageParametry parametry = buildParametry();
        DockerClientConfig clientConfig = createConfig(getProject(), parametry);
        Supplier<DockerClient> clientFactory = () -> {
            return DockerClientBuilder.getInstance(clientConfig).build();
        };
        boolean existsLocally;
        try (DockerClient client = clientFactory.get()) {
            existsLocally = !queryImagesByName(client, parametry.name).isEmpty();
        } catch (IOException e) {
            throw new MojoExecutionException("docker client I/O error", e);
        }
        if (!existsLocally) {
            AbsentImageActor actor = constructActor(clientFactory, directive);
            actor.perform(parametry, directive.parameter);
        }
    }

    static List<Image> queryImagesByName(DockerClient client, String imageName) {
        return client.listImagesCmd().withImageNameFilter(imageName).exec();
    }

    protected RequireImageParametry buildParametry() {
        return RequireImageParametry.newBuilder(imageName)
                .buildTimeout(Durations.parseDuration(buildTimeout, RequireImageParametry.DEFAULT_BUILD_TIMEOUT))
                .pullTimeout(Durations.parseDuration(pullTimeout, RequireImageParametry.DEFAULT_PULL_TIMEOUT))
                .buildArgs(supplyIfNull(buildArgs, Collections::emptyMap))
                .labels(supplyIfNull(buildLabels, Collections::emptyMap))
                .build();
    }

    protected AbsentImageActor constructActor(Supplier<DockerClient> clientFactory, AbsentImageDirective directive) {
        switch (directive.action) {
            case pull:
                return new PullImageActor(getLog(), clientFactory);
            case fail:
                return new FailBuildActor(getLog());
            case build:
                return new BuildImageActor(getLog(), clientFactory, createMavenPropertiesProvider());
            case ignore:
                return new IgnoreImageActor(getLog());
            default:
                throw new IllegalArgumentException(String.format("BUG: unhandled enum constant %s.%s", AbsentImageAction.class.getName(), directive.action));
        }
    }

    MavenProject getProject() {
        return project;
    }

    private static <T> T supplyIfNull(@Nullable T item, Supplier<? extends T> constructor) {
        return item == null ? constructor.get() : item;
    }
}
