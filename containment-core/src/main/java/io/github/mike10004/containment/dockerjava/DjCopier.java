package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.google.common.io.ByteStreams;
import io.github.mike10004.containment.ContainerInfo;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.DockerCopier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class DjCopier implements DockerCopier {

    private final DockerClient client;
    private final ContainerInfo containerInfo;

    public DjCopier(DockerClient client, ContainerInfo containerInfo) {
        this.client = requireNonNull(client);
        this.containerInfo = requireNonNull(containerInfo);
    }

    @Override
    public void copyToContainer(File srcFile, String destinationPathname) throws IOException, ContainmentException {
        try {
            client.copyArchiveToContainerCmd(containerInfo.id())
                    .withHostResource(srcFile.getAbsolutePath())
                    .withRemotePath(destinationPathname)
                    .exec();
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

    @Override
    public void copyFromContainer(String path, File destinationFile, Set<Option> options) throws IOException, ContainmentException {
        try (InputStream in = client.copyArchiveFromContainerCmd(containerInfo.id(), path).exec();
             TarArchiveInputStream tarIn = new TarArchiveInputStream(in)
        ) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                if (entry.getName().equals(FilenameUtils.getName(path))) {
                    try (OutputStream out = new FileOutputStream(destinationFile)) {
                        ByteStreams.copy(tarIn, out);
                    }
                }
            }
        }
    }

}
