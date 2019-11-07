package io.github.mike10004.containment.dockerjava;

import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerExecutor;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.core.DjManagedTestBase;
import io.github.mike10004.containment.core.Tests;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class DjContainerCopierTest extends DjManagedTestBase {

    @ClassRule
    public static final TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void run_copyFilesBeforeStart() throws Exception {
        String content = UUID.randomUUID().toString();
        File file = tempdir.newFile();
        java.nio.file.Files.write(file.toPath(), content.getBytes(UTF_8));
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .commandToWaitIndefinitely()
                .build();
        ContainerSubprocessResult<String> result;
        String copiedFileDestDir = "/root/";
        String pathnameOfFileInContainer = copiedFileDestDir + file.getName();
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.copier().copyToContainer(file, copiedFileDestDir);
            try (StartedContainer container = runnableContainer.start()) {
                ContainerExecutor executor = container.executor();
                result = executor.execute(UTF_8, "cat", pathnameOfFileInContainer);
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        System.out.format("contents of %s: %s%n", pathnameOfFileInContainer, result.stdout().trim());
        assertEquals("text", content, result.stdout().trim());
    }

    @Test
    public void run_copyFilesFromContainer() throws Exception {
        String content = UUID.randomUUID().toString();
        File file = tempdir.newFile();
        Charset charset = UTF_8;
        java.nio.file.Files.write(file.toPath(), content.getBytes(charset));
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .commandToWaitIndefinitely()
                .build();
        String copiedFileDestDir = "/root/";
        String pathnameOfFileInContainer = copiedFileDestDir + file.getName();
        File pulledFile = tempdir.newFile();
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.copier().copyToContainer(file, copiedFileDestDir);
            try (StartedContainer container = runnableContainer.start()) {
                container.copier().copyFromContainer(pathnameOfFileInContainer, pulledFile);
            }
        }
        assertNotEquals("expect file nonempty", 0, pulledFile.length());
        byte[] pulledBytes = java.nio.file.Files.readAllBytes(pulledFile.toPath());
        assertEquals("pulled bytes", file.length(), pulledBytes.length);
        String pulledContent = new String(pulledBytes, charset);
        System.out.format("contents of %s: %s%n", pulledFile, pulledContent);
        assertEquals("text", content, pulledContent);
    }

    @Test
    public void run_unpackArchiveToContainer() throws Exception {
        String content = UUID.randomUUID().toString();
        File ancestor = tempdir.newFolder();
        File intermediate1 = new File(ancestor, "sub1");
        File intermediate2 = new File(intermediate1, "sub2");
        File aFile = new File(intermediate2, "file.txt");
        com.google.common.io.Files.createParentDirs(aFile);
        java.nio.file.Files.write(aFile.toPath(), content.getBytes(UTF_8));
        File tarFile = File.createTempFile("goodthingstoeat", ".tar", tempdir.getRoot());
        TarArchives.packDirectoryInTarArchiveFile(ancestor, tarFile);
        String copyDstRoot = "/root/";
        String pathnameOfFileInContainer = "/root/" + ancestor.toPath().relativize(aFile.toPath()).toString();
        ContainerSubprocessResult<String> result;
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .commandToWaitIndefinitely()
                .build();
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.copier().unpackTarArchiveToContainer(() -> new FileInputStream(tarFile), copyDstRoot);
            try (StartedContainer container = runnableContainer.start()) {
                ContainerExecutor executor = container.executor();
                System.out.format("cat %s%n", pathnameOfFileInContainer);
                result = executor.execute(UTF_8, "cat", pathnameOfFileInContainer);
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        System.out.format("contents of %s: %s%n", pathnameOfFileInContainer, result.stdout().trim());
        assertEquals("text", content, result.stdout().trim());
    }

}