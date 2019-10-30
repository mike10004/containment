package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Verify;
import io.github.mike10004.nitsick.SettingSet;
import org.junit.Assume;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Tests {

    private static final String SYSPROP_PREFIX = "containment-maven-plugin.tests";
    public static final SettingSet Settings = SettingSet.global(SYSPROP_PREFIX);
    private static final AtomicBoolean anyClientsCreated = new AtomicBoolean();

    public static boolean isRealDockerManagerAnyClientsCreated() {
        return anyClientsCreated.get();
    }

    public static DockerManager realDockerManager() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        return new MojoDockerManager(config) {
            @Override
            public DockerClient buildClient() {
                anyClientsCreated.set(true);
                return super.buildClient();
            }
        };
    }

    public static void assumeDestructiveModeEnabled() {
        boolean enabled = Settings.get("destructiveMode.enabled", false);
        Assume.assumeTrue(String.format("set sysprop %s.destructiveMode=true to enable", SYSPROP_PREFIX), enabled);
    }

    public static DockerManager mockDockerManager() {
        return new DockerManager() {
            @Override
            public DockerClient buildClient() {
                throw new UnsupportedOperationException("not supported in mock");
            }

            @Override
            public List<Image> queryImagesByName(DockerClient client, String imageName) {
                throw new UnsupportedOperationException("not supported in mock");
            }
        };
    }

    public static void enforceImageDoesNotExistLocally(DockerManager dockerManager, String remoteImageName) {
        DockerClient client = dockerManager.buildClient();
        List<Image> locals = client.listImagesCmd().withImageNameFilter(remoteImageName).exec();
        if (!locals.isEmpty()) {
            Verify.verify(locals.size() == 1, "expect exactly one image matching %s, but got %s", remoteImageName, locals);
            client.removeImageCmd(locals.get(0).getId()).withForce(true).exec();
            System.out.format("precondition: enforceImageDoesNotExistLocally: removed image %s%n", locals.get(0));
        }
    }
}
