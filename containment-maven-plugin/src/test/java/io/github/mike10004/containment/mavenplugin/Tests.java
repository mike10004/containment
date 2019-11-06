package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Verify;
import io.github.mike10004.nitsick.SettingSet;
import org.junit.Assume;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class Tests {

    private static final String SYSPROP_PREFIX = "containment-maven-plugin.tests";
    public static final SettingSet Settings = SettingSet.system(SYSPROP_PREFIX);
    private static final AtomicInteger anyClientsCreated = new AtomicInteger(0);

    public static boolean isRealDockerManagerAnyClientsCreated() {
        return anyClientsCreated.get() > 0;
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

    public static Supplier<DockerClient> realDockerManager() {
        return () -> {
            anyClientsCreated.incrementAndGet();
            return DockerClientBuilder.getInstance(DOCKER_CLIENT_CONFIG).build();
        };
    }

    public static void assumeDestructiveModeEnabled() {
        boolean enabled = Settings.get("destructiveMode.enabled", false);
        Assume.assumeTrue(String.format("set sysprop %s.destructiveMode=true to enable", SYSPROP_PREFIX), enabled);
    }

    public static Supplier<DockerClient> mockDockerManager() {
        return new Supplier<DockerClient>() {
            @Override
            public DockerClient get() {
                throw new UnsupportedOperationException("not supported by mock");
            }
        };
    }

    public static void enforceImageDoesNotExistLocally(Supplier<DockerClient> dockerManager, String remoteImageName) throws IOException {
        try (DockerClient client = dockerManager.get()) {
            List<Image> locals = client.listImagesCmd().withImageNameFilter(remoteImageName).exec();
            if (!locals.isEmpty()) {
                Verify.verify(locals.size() == 1, "expect exactly one image matching %s, but got %s", remoteImageName, locals);
                client.removeImageCmd(locals.get(0).getId()).withForce(true).exec();
                System.out.format("precondition: enforceImageDoesNotExistLocally: removed image %s%n", locals.get(0));
            }
        }
    }
}
