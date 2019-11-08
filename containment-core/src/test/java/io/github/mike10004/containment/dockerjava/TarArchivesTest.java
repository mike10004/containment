package io.github.mike10004.containment.dockerjava;

import com.google.common.io.CharSource;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class TarArchivesTest {

    @ClassRule
    public static TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testPackFromMemory() throws Exception {
        Map<String, Optional<byte[]>> entries = new LinkedHashMap<>();
        entries.put("foo/", Optional.empty());
        entries.put("foo/bar/", Optional.empty());
        entries.put("foo/bar/baz.txt", Optional.of("baz".getBytes(US_ASCII)));
        File tarFile = tempdir.newFile();
        try (OutputStream os = new FileOutputStream(tarFile)) {
            TarArchives.packItemsInArchive(entries, os);
        }
        assertEntriesEqual(tarFile, "foo/", "foo/bar/", "foo/bar/baz.txt");
    }

    @Test
    public void testPack() throws Exception {
        File root = tempdir.newFolder();
        String[] filepaths = {
                "a/b/c/d.txt",
                "a/b/e.txt",
                "f.txt",
                "g/h/j.txt",
                "g/k.txt",
        };
        String[] dirs = {
                "a/b/c/",
                "a/b/",
                "a/",
                "g/h/",
                "g/",
        };
        for (String filepath : filepaths) {
            File f = new File(root, filepath);
            com.google.common.io.Files.createParentDirs(f);
            java.nio.file.Files.write(f.toPath(), f.getName().getBytes(UTF_8));
        }
        File tarFile = new File(tempdir.newFolder(), "packed.tar");
        TarArchives.packDirectoryInTarArchiveFile(root, tarFile);
        System.out.println(tarFile);
        Set<String> expected = new TreeSet<>(Arrays.asList(filepaths));
        expected.addAll(Arrays.asList(dirs));
        assertEntriesEqual(expected, tarFile);
    }

    private static void assertEntriesEqual(File tarFile, String...expected) throws IOException, InterruptedException, TimeoutException {
        assertEntriesEqual(new HashSet<>(Arrays.asList(expected)), tarFile);
    }

    private static void assertEntriesEqual(Set<String> expected, File tarFile) throws IOException, InterruptedException, TimeoutException {
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = Subprocess.running("tar")
                    .args("tf", tarFile.getAbsolutePath())
                    .build()
                    .launcher(processTracker)
                    .outputStrings(Charset.defaultCharset())
                    .launch().await(30, TimeUnit.SECONDS);
        }
        System.out.println(result);
        assertEquals("exit code", 0, result.exitCode());
        System.out.println(result.content().stdout());
        Set<String> tarTfLines = new TreeSet<>(CharSource.wrap(result.content().stdout()).readLines());
        assertEquals("lines", expected, tarTfLines);
    }
}
