package io.github.mike10004.containment;

import com.google.common.io.ByteSource;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DjContainerRunnerTest {

    @Test
    public void execute_setContainerEnvironmentVariables() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .env("FOO", "bar")
                .commandToWaitIndefinitely()
                .build();

        DockerSubprocessResult<String> result;
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
                DockerExecutor executor = new DockerExecExecutor(container.id(), Collections.emptyMap(), UTF_8);
                result = executor.execute("printenv");
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        Tests.assertStdoutHasLine(result, "FOO=bar");
    }

    @Test
    public void run_exposePorts() throws Exception {
        int httpdPort = 80;
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForHttpdTest())
                .expose(httpdPort)
                .build();
        String result;
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
                List<PortMapping> ports = container.fetchPorts();
                PortMapping httpdExposedPortMapping = ports.stream().filter(p -> p.containerPort == httpdPort).findFirst().orElseThrow(() -> new IllegalStateException("no mapping for port 80 found"));
                assertTrue("exposed", httpdExposedPortMapping.isExposed());
                assertNotNull(httpdExposedPortMapping.host);
                URL url = new URL("http", "localhost", httpdExposedPortMapping.host.getPort(), "/");
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