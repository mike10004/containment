package io.github.mike10004.containment;

import com.google.common.io.ByteSource;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class DjContainerRunnerTest {

    @Test
    public void execute_environmentVariable() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder("busybox")
//                .command(Arrays.asList("tail", "-f", "/dev/null"))
                .command(Arrays.asList("echo", "$FOO"))
                .build();

        DockerExecResult<String> result;
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
//                DockerExecutor executor = new DockerSubprocessExecutor(container.id(), new HashMap<>(), UTF_8);
//                result = executor.execute("echo", "$FOO");
            }
        }
//        System.out.println(result);
//        assertEquals("result content", "bar", result.stdout().trim());
//        assertEquals("process exit code", 0, result.exitCode());
    }

    @Test
    public void execute_exposePorts() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder("httpd:2.4")
                .expose(80)
                .build();
        String result;
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
                List<PortMapping> ports = container.fetchPorts();
                PortMapping httpPort = ports.stream().filter(p -> p.containerPort == 80).findFirst().orElseThrow(() -> new IllegalStateException("no mapping for port 80 found"));
                assertTrue("exposed", httpPort.isExposed());
                assertNotNull(httpPort.host);
                URL url = new URL("http", "localhost", httpPort.host.getPort(), "/");
                byte[] content = new JreClient().fetchPageContent(url);
                result = new String(content, UTF_8);
            }
        }
        System.out.println(result);
        assertEquals("page text", "<html><body><h1>It works!</h1></body></html>", result.trim());
    }

    private static class JreClient {

        public byte[] fetchPageContent(URL url) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            ByteSource contentSource;
            if (conn.getResponseCode() / 100 == 2) {
                contentSource = new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return conn.getInputStream();
                    }
                };
            } else {
                contentSource = new ByteSource() {

                    @Override
                    public InputStream openStream() {
                        InputStream in = conn.getErrorStream();
                        return in == null ? new ByteArrayInputStream(new byte[0]) : in;
                    }
                };
            }
            try {
                return contentSource.read();
            } finally {
                conn.disconnect();
            }
        }

    }
}