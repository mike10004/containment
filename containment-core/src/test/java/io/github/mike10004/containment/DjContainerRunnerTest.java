package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.google.common.io.ByteSource;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DjContainerRunnerTest {

    @ClassRule
    public static final TemporaryFolder tempdir = new TemporaryFolder();

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

    private static class PreCopier implements PreStartAction {

        private final DockerClient client;
        private final File srcFile;
        private final String destination;

        public PreCopier(DockerClient client, File srcFile, String destination) {
            this.client = client;
            this.srcFile = srcFile;
            this.destination = destination;
        }

        @Override
        public void perform(CreatedContainer unstartedContainer) {
            client.copyArchiveToContainerCmd(unstartedContainer.id())
                    .withHostResource(srcFile.getAbsolutePath())
                    .withRemotePath(destination)
                    .exec();
        }
    }

    @Test
    public void run_copyFilesBeforeStart() throws Exception {
        String content = UUID.randomUUID().toString();
        File file = tempdir.newFile();
        java.nio.file.Files.write(file.toPath(), content.getBytes(UTF_8));
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .commandToWaitIndefinitely()
                .build();
        DockerSubprocessResult<String> result;
        DockerClient client = TestDockerManager.getInstance().buildClient();
        String copiedFileDestDir = "/root/";
        String pathnameOfFileInContainer = copiedFileDestDir + file.getName();
        PreCopier copier = new PreCopier(client, file, copiedFileDestDir);
        try (ContainerRunner runner = new DjContainerRunner(client);
            RunnableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.execute(copier);
            try (RunningContainer container = runnableContainer.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.id(), Collections.emptyMap(), UTF_8);
                result = executor.execute("cat", pathnameOfFileInContainer);
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        System.out.format("contents of %s: %s%n", pathnameOfFileInContainer, result.stdout().trim());
        assertEquals("text", content, result.stdout().trim());
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
                assertTrue("exposed", httpdExposedPortMapping.isBound());
                assertNotNull(httpdExposedPortMapping.host);
                URL url = new URL("http", "localhost", httpdExposedPortMapping.host.getPort(), "/");
                byte[] content = new JreClient().fetchPageContent(url);
                result = new String(content, UTF_8);
            }
        }
        System.out.println(result);
        assertEquals("page text", "<html><body><h1>It works!</h1></body></html>", result.trim());
    }

    @SuppressWarnings("SqlDialectInspection")
    @Test
    public void run_mysql() throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        int mysqlPort = 3306;
        String password = "sUpers3cret";
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForMysqlTest())
                .expose(mysqlPort)
                .env("MYSQL_ROOT_PASSWORD", password)
                // entrypoint script supports just adding options as the command
                .command(Arrays.asList("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci", "--bind-address=0.0.0.0"))
                .build();
        List<String> colors = new ArrayList<>();
        try (DjContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance().buildClient())) {
            try (RunningContainer container = runner.run(parametry)) {
                int hostPort = container.fetchPorts().stream()
                        .filter(PortMapping::isBound)
                        .map(pm -> pm.host)
                        .filter(Objects::nonNull)
                        .mapToInt(FullSocketAddress::getPort)
                        .findFirst().orElseThrow(() -> new AssertionError("port not bound"));
                String jdbcUrl = "jdbc:mysql://127.0.0.1:" + hostPort + "/";
                System.out.println("connecting on " + jdbcUrl);
                String dbName = "widget_factory";
                String requiredLineSuffix = "Server socket created on IP: '0.0.0.0'.";
                System.out.format("awaiting line ending in %s%n", requiredLineSuffix);
                Duration mysqlStartupTimeout = Tests.Settings.timeouts().get("mysqlStartup", Duration.ofMinutes(5));
                boolean logMessageAppeared = container.followStderr(BlockableLogFollower.untilLine(line -> line.endsWith(requiredLineSuffix), UTF_8))
                        .await(mysqlStartupTimeout);
                System.out.format("saw line ending in %s: %s%n", requiredLineSuffix, logMessageAppeared);
                try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, "root", password)) {
                    System.out.format("connected to %s%n", jdbcUrl);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA " + dbName);
                    }
                }
                jdbcUrl += dbName;
                try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, "root", password)) {
                    System.out.format("connected to %s%n", jdbcUrl);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE TABLE `widget` (\n" +
                                "  `widget_id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                                "  `color` varchar(31) NOT NULL DEFAULT '',\n" +
                                "  PRIMARY KEY (`widget_id`)\n" +
                                ") ENGINE=InnoDB;\n");
                        stmt.execute("INSERT INTO widget (color) VALUES ('red'), ('blue'), ('green')");
                        try (ResultSet rs = stmt.executeQuery("SELECT * FROM widget WHERE 1")) {
                            while (rs.next()) {
                                colors.add(rs.getString(2));
                            }
                        }
                    }
                }
            }
        }
        System.out.format("fetched widgets: %s%n", colors);
        assertEquals(Arrays.asList("red", "blue", "green"), colors);
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