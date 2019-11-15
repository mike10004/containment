package io.github.mike10004.containment.lifecycle;

import com.google.common.primitives.Bytes;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class LifecycledResourceTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void inScope() throws IOException {
        AtomicReference<File> fileRef = new AtomicReference<>();
        byte[] head = "hello".getBytes();
        byte[] tail = ", world".getBytes();
        LifecycledResource<BufferedOutputStream> resource = new LifecycledResource<BufferedOutputStream>() {

            private BufferedOutputStream os;

            @Override
            public Provision<BufferedOutputStream> request() {
                try {
                    File f = File.createTempFile("thisistemporary", ".tmp", temporaryFolder.getRoot());
                    fileRef.set(f);
                    os = new BufferedOutputStream(new FileOutputStream(f), 1024);
                    return Computation.succeeded(os);
                } catch (IOException e) {
                    return Provision.failed(e);
                }
            }

            @Override
            public void finishLifecycle() {
                if (os != null) {
                    try {
                        os.write(tail);
                        os.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        try (ScopedResource<BufferedOutputStream> scope = resource.inScope()) {
            scope.acquire().write(head);
        }
        byte[] expected = Bytes.concat(head, tail);
        File f = fileRef.get();
        assertNotNull("file was set", f);
        byte[] fileBytes = java.nio.file.Files.readAllBytes(f.toPath());
        // if the lifecycle was not finished, then this will not have been written
        assertArrayEquals("file content", expected, fileBytes);
    }
}