package io.github.mike10004.containment.dockerjava;

import com.google.common.collect.Ordering;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility methods relating to tar archive data.
 */
public class TarArchives {

    public TarArchives() {}

    /**
     * Archives files from a directory, creating a tar file.
     * @param directory pathname of the directory whose contents are to be archived
     * @param destinationFile pathname of the destination tar file
     * @throws IOException on I/O error
     */
    public static void packDirectoryInTarArchiveFile(File directory, File destinationFile) throws IOException {
        try (OutputStream fos = new FileOutputStream(destinationFile)) {
            packDirectoryInTarArchive(directory, fos);
        }
    }

    /**
     * Archives files in the given directory, writing content to an output stream.
     * @param directory the directory whose contents are to be packed
     * @param outputStream destination output stream
     * @throws IOException on I/O error
     */
    public static void packDirectoryInTarArchive(File directory, OutputStream outputStream) throws IOException {
        Path root = directory.toPath();
        List<File> filesInDirectory = java.nio.file.Files.walk(root)
                .filter(p -> !root.equals(p))
                .map(p -> {
                    return root.resolve(p).toFile();
                }).sorted(Ordering.<String>natural().onResultOf(File::getAbsolutePath))
                .collect(Collectors.toList());
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(outputStream)) {
            for (File file : filesInDirectory) {
                String entryName = root.relativize(file.toPath()).toString();//.relativize(root).toString();
                ArchiveEntry entry = tos.createArchiveEntry(file, entryName);
                tos.putArchiveEntry(entry);
                if (file.isFile()) {
                    try (InputStream entryFileInput = java.nio.file.Files.newInputStream(file.toPath())) {
                        IOUtils.copy(entryFileInput, tos);
                    } finally {
                        tos.closeArchiveEntry();
                    }
                }
            }
            tos.finish();
        }
    }
}
