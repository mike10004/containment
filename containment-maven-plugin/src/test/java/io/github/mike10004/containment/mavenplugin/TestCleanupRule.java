package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.google.common.collect.ImmutableMap;
import org.junit.rules.ExternalResource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestCleanupRule extends ExternalResource {

    public static final String INCLUDE_LABEL_NAME = "containment-maven-plugin-tests-include";
    public static final String INCLUDE_LABEL_VALUE = "true";
    public static final String INCLUDE_REPOSITORY_VALUE = "containment-tests";

    @Override
    protected void after() {
        if (!Tests.isRealDockerManagerAnyClientsCreated()) {
            return;
        }
        DockerClient client = Tests.realDockerManager().buildClient();
        doWithCatch(() -> removeImagesBylabel(client));
        doWithCatch(() -> removeImagesByTag(client));
    }

    private void removeImagesByTag(DockerClient client) {
        List<Image> images = client.listImagesCmd()
                .withImageNameFilter(INCLUDE_REPOSITORY_VALUE + "/*")
                .exec();
        removeImages(client, images);
    }

    private void removeImagesBylabel(DockerClient client) {
        List<Image> images = client.listImagesCmd().withLabelFilter(ImmutableMap.of(INCLUDE_LABEL_NAME, INCLUDE_LABEL_VALUE)).exec();
        System.out.format("%d images found with label %s=%s%n", images.size(), INCLUDE_LABEL_NAME, INCLUDE_LABEL_VALUE);
        removeImages(client, images);
    }

    private void removeImages(DockerClient client, Iterable<Image> images) {
        for (Image image : images) {
            doWithCatch(() -> {
                System.out.format("removing image %s (created %s)%n", image.getId(), image.getCreated());
                client.removeImageCmd(image.getId()).withForce(true).exec();
                System.out.format("removed image %s%n", image.getId());
            });
        }
    }

    private Optional<Optional<Void>> doWithCatch(Runnable action, String...labels) {
        return doWithCatch(() -> {
            action.run();
            return null;
        }, labels);
    }

    /**
     * @return an optional containing the return value of the action, or null if exception thrown by the action
     */
    private <T> Optional<Optional<T>> doWithCatch(Callable<T> action, String...labels) {
        try {
            return Optional.of(Optional.ofNullable(action.call()));
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, e, () -> String.format("caught exception while executing %s", Arrays.toString(labels)));
            return Optional.empty();
        }
    }
}
