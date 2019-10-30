package io.github.mike10004.containment.mavenplugin;

import io.github.mike10004.containment.DockerManager;
import io.github.mike10004.nitsick.Durations;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;

import java.util.Properties;
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
    private String name;

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
     * <pre>
     *     &lt;buildArgs&t;
     *       &lt;buildArg&gt;
     *         &lt;name&gt;name1&lt;/name&gt;
     *         &lt;value&gt;value1&lt;/value&gt;
     *       &lt;/buildArg&gt;
     *       &lt;buildArg&gt;
     *         &lt;name&gt;name2&lt;/name&gt;
     *         &lt;value&gt;value2&lt;/value&gt;
     *       &lt;/buildArg&gt;
     *     &lt;/buildArgs&t;
     * </pre>
     */
    @Parameter
    private Properties buildArgs;

    /**
     * Labels to apply to a freshly-built image.
     *
     * Use markup like the following:
     * <pre>
     *     &lt;imageLabels&t;
     *       &lt;label&gt;
     *         &lt;name&gt;name1&lt;/name&gt;
     *         &lt;value&gt;value1&lt;/value&gt;
     *       &lt;/label&gt;
     *       &lt;label&gt;
     *         &lt;name&gt;name2&lt;/name&gt;
     *         &lt;value&gt;value2&lt;/value&gt;
     *       &lt;/label&gt;
     *     &lt;/imageLabels&t;
     * </pre>
     */
    @Parameter
    private Properties imageLabels;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void execute() throws MojoExecutionException {
        requireNonNull(absentImageAction, "absentImageAction");
        AbsentImageDirective directive = AbsentImageDirective.parse(absentImageAction);
        RequireImageParametry parametry = buildParametry();
        DockerManager dockerManager = MojoDockerManager.fromParametry(project, parametry);
        boolean existsLocally = dockerManager.queryImageExistsLocally(parametry.name);
        if (!existsLocally) {
            AbsentImageActor actor = determineActor(dockerManager, directive);
            actor.perform(parametry, directive.parameter);
        }
    }

    protected RequireImageParametry buildParametry() {
        return RequireImageParametry.newBuilder(name)
                .buildTimeout(Durations.parseDuration(buildTimeout, RequireImageParametry.DEFAULT_BUILD_TIMEOUT))
                .pullTimeout(Durations.parseDuration(pullTimeout, RequireImageParametry.DEFAULT_PULL_TIMEOUT))
                .buildArgs(supplyIfNull(buildArgs, Properties::new))
                .labels(supplyIfNull(imageLabels, Properties::new))
                .build();
    }

    protected AbsentImageActor determineActor(DockerManager dockerManager, AbsentImageDirective directive) {
        switch (directive.action) {
            case pull:
                return new PullImageActor(getLog(), dockerManager);
            case fail:
                return new FailBuildActor(getLog());
            case build:
                return new BuildImageActor(getLog(), dockerManager, createMavenPropertiesProvider());
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
