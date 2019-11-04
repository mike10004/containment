package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import io.github.mike10004.containment.DjDockerManager;
import io.github.mike10004.containment.ImageSpecifier;
import io.github.mike10004.containment.Uuids;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PullImageActorTest {

    @ClassRule
    public static final TestCleanupRule cleanupRule = new TestCleanupRule();

    @Test
    public void perform_pullHelloWorld_untagged() throws Exception {
        Random random = new Random("PullImageActorTest_perform_pullHelloWorld_untagged".hashCode());
        String name = new ImageSpecifier(Uuids.randomUuidString(random))
                .withRepository(TestCleanupRule.INCLUDE_REPOSITORY_VALUE)
                .toString();
        test_perform_pullHelloWorld(name, null);
    }

    @Test
    public void perform_pullHelloWorld_tag_linux() throws Exception {
        Random random = new Random("PullImageActorTest_perform_pullHelloWorld_tag_linux".hashCode());
        String name = new ImageSpecifier(Uuids.randomUuidString(random))
                .withRepository(TestCleanupRule.INCLUDE_REPOSITORY_VALUE)
                .toString();
        test_perform_pullHelloWorld(name, "linux");
    }

    private void test_perform_pullHelloWorld(String name, String tag) throws Exception {
        Tests.assumeDestructiveModeEnabled();
        String bareImageName = Tests.getSetting("helloWorldImageName", "hello-world");
        ImageSpecifier remoteImageSpec = new ImageSpecifier(bareImageName, tag);
        String remoteImageName = remoteImageSpec.toString();
        DjDockerManager dockerManager = Tests.realDockerManager();
        Tests.enforceImageDoesNotExistLocally(dockerManager, remoteImageSpec.withDefaultTag("latest").toString());
        RequireImageParametry parametry = RequireImageParametry.newBuilder(name).build();
        LogBucket log = new LogBucket();
        PullImageActor actor = new PullImageActor(log, dockerManager);
        actor.perform(parametry, remoteImageName);
        confirmTaggedImageExists(dockerManager, parametry.name);
    }

    private void confirmTaggedImageExists(DjDockerManager dockerManager, String name) {
        DockerClient client = dockerManager.openClient();
        List<Image> images = dockerManager.queryImagesByName(client, name);
        if (images.isEmpty()) {
            images = client.listImagesCmd().withShowAll(true).exec();
            fail("image with name " + name + " not found among " + images);
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