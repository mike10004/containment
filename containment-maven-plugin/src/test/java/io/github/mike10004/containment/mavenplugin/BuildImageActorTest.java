package io.github.mike10004.containment.mavenplugin;

import com.github.dockerjava.api.DockerClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import io.github.mike10004.containment.DockerManager;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuildImageActorTest {

    @ClassRule
    public static final TemporaryFolder tempdir = new TemporaryFolder();

    @ClassRule
    public static final TestCleanupRule cleanupRule = new TestCleanupRule();

    private Random random;

    @Before
    public void setUp() {
        random = new Random("BuildImageActorTest".hashCode());
    }

    @Test
    public void perform_basic() throws Exception {
        DockerManager dockerManager = Tests.realDockerManager();
        BuildImageActor actor = new BuildImageActor(new LogBucket(), dockerManager, x -> null);
        String imageName = Uuids.randomUuidString(random);
        RequireImageParametry parametry = RequireImageParametry.newBuilder(imageName)
                .label(TestCleanupRule.INCLUDE_LABEL_NAME, TestCleanupRule.INCLUDE_LABEL_VALUE)
                .build();
        File dockerfileDir = tempdir.newFolder();
        File srcDockerfile = new File(getClass().getResource("/BuildImageActorTest/perform_basic/Dockerfile").toURI());
        java.nio.file.Files.copy(srcDockerfile.toPath(), dockerfileDir.toPath().resolve("Dockerfile"));
        actor.perform(parametry, dockerfileDir.getAbsolutePath());
        confirmBasicImageBuilt(dockerManager, imageName);
    }

    private void confirmBasicImageBuilt(DockerManager dockerManager, String imageName) throws IOException {
        DockerClient client = dockerManager.buildClient();
        File tarFile = new File(tempdir.newFolder(), imageName + ".tar");

        try (InputStream in = client.saveImageCmd(imageName).exec();
             OutputStream out = new FileOutputStream(tarFile)) {
            ByteStreams.copy(in, out);
        }
        File scratchDir = tempdir.newFolder();
        List<File> layerTarFiles = new ArrayList<>();
        int numLayerTarFiles = 0;
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(tarFile))) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextTarEntry()) != null) {
                String entryName = entry.getName();
                if ("layer.tar".equals(FilenameUtils.getName(entryName))) {
                    numLayerTarFiles++;
                    File layerTarFile = new File(scratchDir, String.format("layer-%d.tar", numLayerTarFiles));
                    try (OutputStream layerTarOut = new FileOutputStream(layerTarFile)) {
                        long layerTarSize = ByteStreams.copy(tin, layerTarOut);
                        System.out.format("%d bytes extracted for %s%n", layerTarSize, layerTarFile);
                    }
                    layerTarFiles.add(layerTarFile);
                }
            }
        }
        boolean testfileFound = false;
        // the layer containing /testfile is pretty small, so let's look at that one first
        layerTarFiles.sort(Ordering.<Long>natural().onResultOf(File::length).reversed());
        for (File layerTarFile : layerTarFiles) {
            Collection<String> entryNames = collectEntryNames(layerTarFile);
            if (entryNames.contains("testfile")) {
                testfileFound = true;
                break;
            }
        }
        assertTrue("/testfile not found in image filesystem", testfileFound);
    }

    private static List<String> collectEntryNames(File tarFile) throws IOException {
        List<String> allEntryNames = new ArrayList<>(128);
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(tarFile))) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextTarEntry()) != null) {
                String entryName = entry.getName();
                allEntryNames.add(entryName);
            }
        }
        return allEntryNames;
    }

    @Test(expected = MojoExecutionException.class)
    public void resolveDockerfileDir_fail() throws Exception {
        File basedir = tempdir.newFolder();
        testResolveDockerfileDir(basedir);
    }

    @Test
    public void resolveDockerfileDir_succeed() throws Exception {
        File basedir = tempdir.newFolder();
        File dockerfile = new File(basedir, "src/test/docker/Dockerfile");
        com.google.common.io.Files.createParentDirs(dockerfile);
        com.google.common.io.Files.touch(dockerfile);
        File dockerfileDir = testResolveDockerfileDir(basedir);
        assertEquals("not expected location of dockerfile dir", dockerfileDir.getCanonicalFile(), dockerfile.getParentFile().getCanonicalFile());
    }

    private File testResolveDockerfileDir(File basedir) throws MojoExecutionException  {
        Function<String, String> props = ImmutableMap.of("project.basedir", basedir.getAbsolutePath())::get;
        BuildImageActor actor = new BuildImageActor(new LogBucket(), Tests.mockDockerManager(), props);
        File dir = actor.resolveDefaultDockerfileDir();
        return dir;
    }
}