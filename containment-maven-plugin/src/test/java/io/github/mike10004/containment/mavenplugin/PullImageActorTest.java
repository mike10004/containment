package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import io.github.mike10004.containment.ImageSpecifier;
import io.github.mike10004.containment.Uuids;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PullImageActorTest {

    @ClassRule
    public static final TestCleanupRule cleanupRule = new TestCleanupRule();

    @Test
    public void perform_pullHelloWorld_untagged() throws Exception {
        Random random = new Random("perform_pullHelloWorld_untagged".hashCode());
        String name = ImageSpecifier.fromNameOnly(Uuids.randomUuidString(random))
                .withRepository(TestCleanupRule.INCLUDE_REPOSITORY_VALUE)
                .toString();
        test_perform_pullHelloWorld(name, null);
    }

    @Test
    public void perform_pullHelloWorld_tag_tagged() throws Exception {
        Random random = new Random("perform_pullHelloWorld_tag_tagged".hashCode());
        String name = ImageSpecifier.fromNameOnly(Uuids.randomUuidString(random))
                .withRepository(TestCleanupRule.INCLUDE_REPOSITORY_VALUE)
                .toString();
        test_perform_pullHelloWorld(name, Tests.getTest1ImageTag());
    }

    private void test_perform_pullHelloWorld(String name, String tag) throws Exception {
        Tests.assumeDestructiveModeEnabled();
        String bareImageName = Tests.getTest1BareImageName();
        ImageSpecifier remoteImageSpec = ImageSpecifier.fromNameAndTag(bareImageName, tag);
        String remoteImageName = remoteImageSpec.toString();
        Supplier<DockerClient> dockerManager = Tests.realDockerManager();
        Tests.enforceImageDoesNotExistLocally(dockerManager, remoteImageSpec.withDefaultTag("latest").toString());
        RequireImageParametry parametry = RequireImageParametry.newBuilder(name).build();
        LogBucket log = new LogBucket();
        PullImageActor actor = new PullImageActor(log, dockerManager);
        actor.perform(parametry, remoteImageName);
        confirmTaggedImageExists(dockerManager, parametry.name);
    }

    private void confirmTaggedImageExists(Supplier<DockerClient> dockerManager, String name) throws IOException {
        try (DockerClient client = dockerManager.get()) {
            List<Image> images = RequireImageMojo.queryImagesByName(client, name);
            if (images.isEmpty()) {
                images = client.listImagesCmd().withShowAll(true).exec();
                fail("image with name " + name + " not found among " + images);
            }
        }
    }

    @Test
    public void resolveRemoteName() {
        String[][] testCases = {
                // localName, repositoryImageName, expected
                {"x", "y", "y"},
                {"x", null, "x"},
        };
        for (String[] testCase : testCases) {
            LogBucket log = new LogBucket();
            String name = testCase[0], repositoryImageName = testCase[1], expected = testCase[2];
            RequireImageParametry p = RequireImageParametry.newBuilder(name)
                    .build();
            String actual = new PullImageActor(log, Tests.mockDockerManager()).resolveRemoteName(p, repositoryImageName);
            assertEquals("name to pull from remote", expected, actual);
        }
    }

    @Test(expected = NullPointerException.class)
    public void resolveRemoteName_nullParametry() {
        new PullImageActor(new LogBucket(), Tests.mockDockerManager()).resolveRemoteName(null, "anything");
    }
}