package io.github.mike10004.containment.dockerjava;

import com.google.common.collect.Ordering;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Static utility methods relating to tar archive data.
 */
public class TarArchives {

    public TarArchives() {
    }

    /**
     * Archives files from a directory, creating a tar file.
     *
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
     *
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
                    }
                }
                tos.closeArchiveEntry();
            }
            tos.finish();
        }
    }

    public static void packItemsInArchive(Map<String, Optional<byte[]>> entries, OutputStream outputStream) throws IOException {
        @SuppressWarnings("ConstantConditions") // for some reason the inspector thinks the key might be null
        List<Map.Entry<String, Optional<byte[]>>> sortedEntries = entries.entrySet().stream()
                .sorted(Ordering.<String>natural().onResultOf(Map.Entry::getKey))
                .collect(Collectors.toList());
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(outputStream)) {
            long modTimeMillis = System.currentTimeMillis();
            for (Map.Entry<String, Optional<byte[]>> item : sortedEntries) {
                String entryName = item.getKey();
                @Nullable byte[] fileContents = item.getValue().orElse(null);
                int numBytes = fileContents == null ? 0 : fileContents.length;
                ArchiveEntry archiveEntry = new CustomTarArchiveEntry(entryName, numBytes, modTimeMillis);
                tos.putArchiveEntry(archiveEntry);
                if (fileContents != null) {
                    tos.write(fileContents);
                }
                tos.closeArchiveEntry();

            }
            tos.finish();
        }
    }

    /*
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements.  See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership.  The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License.  You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing,
     * software distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied.  See the License for the
     * specific language governing permissions and limitations
     * under the License.
     */
    private static class CustomTarArchiveEntry extends TarArchiveEntry {
        /**
         * Construct an entry for a file. File is set to file, and the
         * header is constructed from information from the file.
         *
         * <p>The entry's name will be the value of the {@code fileName}
         * argument with all file separators replaced by forward slashes
         * and leading slashes as well as Windows drive letters stripped.
         * The name will end in a slash if the {@code file} represents a
         * directory.</p>
         *
         * @param file The file that the entry represents.
         * @param fileName the name to be used for the entry.
         */
        public CustomTarArchiveEntry(final String fileName, long numBytes, long modTimeMillis) {
            super(fileName, false);
            final String normalizedName = normalizeFileName(fileName, false);
            boolean directory = FilenameUtils.normalize(fileName, true).endsWith("/");
            if (directory) {
                this.setMode(DEFAULT_DIR_MODE);
                // this.linkFlag = LF_DIR; // set in super constructor
                final int nameLength = normalizedName.length();
                String name;
                if (nameLength == 0 || normalizedName.charAt(nameLength - 1) != '/') {
                    name = normalizedName + "/";
                } else {
                    name = normalizedName;
                }
                setName(name);
            } else {
                setMode(DEFAULT_FILE_MODE);
                // this.linkFlag = LF_NORMAL;  set in super constructor
                setSize(numBytes);
                setName(normalizedName);
            }

            setModTime(modTimeMillis / MILLIS_PER_SECOND);
            setUserName("");
        }

        /**
         * Strips Windows' drive letter as well as any leading slashes,
         * turns path separators into forward slahes.
         */
        private static String normalizeFileName(String fileName,
                                                final boolean preserveAbsolutePath) {
            if (!preserveAbsolutePath) {
                final String osname = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

                // Strip off drive letters!
                // REVIEW Would a better check be "(File.separator == '\')"?

                if (osname.startsWith("windows")) {
                    if (fileName.length() > 2) {
                        final char ch1 = fileName.charAt(0);
                        final char ch2 = fileName.charAt(1);

                        if (ch2 == ':'
                                && (ch1 >= 'a' && ch1 <= 'z'
                                || ch1 >= 'A' && ch1 <= 'Z')) {
                            fileName = fileName.substring(2);
                        }
                    }
                } else if (osname.contains("netware")) {
                    final int colon = fileName.indexOf(':');
                    if (colon != -1) {
                        fileName = fileName.substring(colon + 1);
                    }
                }
            }

            fileName = fileName.replace(File.separatorChar, '/');

            // No absolute pathnames
            // Windows (and Posix?) paths can start with "\\NetworkDrive\",
            // so we loop on starting /'s.
            while (!preserveAbsolutePath && fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }
            return fileName;
        }

    }
}
