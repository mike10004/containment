package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Verify;
import io.github.mike10004.containment.ContainerMonitor;
import io.github.mike10004.containment.DockerClientBuilder;
import io.github.mike10004.containment.DockerManager;
import io.github.mike10004.containment.ShutdownHookContainerMonitor;
import io.github.mike10004.nitsick.SettingSet;
import org.junit.Assume;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Tests {

    private static final String SYSPROP_PREFIX = "containment-maven-plugin.tests";
    public static final SettingSet Settings = SettingSet.system(SYSPROP_PREFIX);
    private static final AtomicBoolean anyClientsCreated = new AtomicBoolean();

    public static boolean isRealDockerManagerAnyClientsCreated() {
        return anyClientsCreated.get();
    }

    public static String getSetting(String identifier, String defaultValue) {
        return getSetting(identifier, Function.identity(), defaultValue);
    }

    public static <T> T getSetting(String identifier, Function<? super String, ? extends T> transform, T defaultValue) {
        String value = Settings.get(identifier);
        if (value == null) {
            return defaultValue;
        }
        return transform.apply(value);
    }

    private static final DockerClientConfig DOCKER_CLIENT_CONFIG = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    private static final ContainerMonitor SINGLETON_CONTAINER_MONITOR = new ShutdownHookContainerMonitor(() -> DockerClientBuilder.getInstance(DOCKER_CLIENT_CONFIG).build());
    private static final DockerManager SINGLETON_MANAGER = new MojoDockerManager(DOCKER_CLIENT_CONFIG, SINGLETON_CONTAINER_MONITOR);

    public static DockerManager realDockerManager() {
        return SINGLETON_MANAGER;
    }

    public static void assumeDestructiveModeEnabled() {
        boolean enabled = Settings.get("destructiveMode.enabled", false);
        Assume.assumeTrue(String.format("set sysprop %s.destructiveMode=true to enable", SYSPROP_PREFIX), enabled);
    }

    public static DockerManager mockDockerManager() {
        return new DockerManager() {
            @Override
            public DockerClient openClient() {
                throw new UnsupportedOperationException("not supported by mock");
            }

            @Override
            public List<Image> queryImagesByName(DockerClient client, String imageName) {
                throw new UnsupportedOperationException("not supported by mock");
            }

            @Override
            public ContainerMonitor getContainerMonitor() {
                throw new UnsupportedOperationException("not supported by mock");
            }
        };
    }

    public static void enforceImageDoesNotExistLocally(DockerManager dockerManager, String remoteImageName) {
        DockerClient client = dockerManager.openClient();
        List<Image> locals = client.listImagesCmd().withImageNameFilter(remoteImageName).exec();
        if (!locals.isEmpty()) {
            Verify.verify(locals.size() == 1, "expect exactly one image matching %s, but got %s", remoteImageName, locals);
            client.removeImageCmd(locals.get(0).getId()).withForce(true).exec();
            System.out.format("precondition: enforceImageDoesNotExistLocally: removed image %s%n", locals.get(0));
        }
    }
}
