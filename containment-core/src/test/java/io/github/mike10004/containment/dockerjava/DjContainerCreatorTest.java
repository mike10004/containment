package io.github.mike10004.containment.dockerjava;

import com.google.common.io.ByteSource;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerPort;
import io.github.mike10004.containment.DockerExecutor;
import io.github.mike10004.containment.DockerSubprocessResult;
import io.github.mike10004.containment.Durations;
import io.github.mike10004.containment.FullSocketAddress;
import io.github.mike10004.containment.ImageSpecifier;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.core.TestDockerManager;
import io.github.mike10004.containment.core.Tests;
import io.github.mike10004.containment.subprocess.DockerExecExecutor;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DjContainerCreatorTest {

    @ClassRule
    public static final TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void execute_setContainerEnvironmentVariables() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .env("FOO", "bar")
                .commandToWaitIndefinitely()
                .build();

        DockerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), Collections.emptyMap(), UTF_8);
                result = executor.execute("printenv");
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        Tests.assertStdoutHasLine(result, "FOO=bar");
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
        String copiedFileDestDir = "/root/";
        String pathnameOfFileInContainer = copiedFileDestDir + file.getName();
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.copier().copyToContainer(file, copiedFileDestDir);
            try (StartedContainer container = runnableContainer.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), Collections.emptyMap(), UTF_8);
                result = executor.execute("cat", pathnameOfFileInContainer);
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
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
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
    public void run_exposePorts() throws Exception {
        int httpdPort = 80;
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForHttpdTest())
                .bindPort(httpdPort)
                .build();
        String result;
        try (ContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                List<ContainerPort> ports = container.fetchPorts();
                ContainerPort httpdExposedPortMapping = ports.stream().filter(p -> p.number() == httpdPort).findFirst().orElseThrow(() -> new IllegalStateException("no mapping for port 80 found"));
                assertTrue("exposed", httpdExposedPortMapping.isBound());
                FullSocketAddress hostBinding = httpdExposedPortMapping.hostBinding();
                assertNotNull(hostBinding);
                URL url = new URL("http", "localhost", hostBinding.getPort(), "/");
                byte[] content = new JreClient().fetchPageContent(url);
                result = new String(content, UTF_8);
            }
        }
        System.out.println(result);
        assertEquals("page text", "<html><body><h1>It works!</h1></body></html>", result.trim());
    }

    private static ImageSpecifier getImageForMysqlTest() {
        return Tests.getImageForTest("run_mysql.image", "mariadb:10.4");
    }

    @SuppressWarnings("SqlDialectInspection")
    @Test
    public void run_mysql() throws Exception {
        boolean verboseWait = Tests.Settings.get("run_mysql.verboseWait", false);
        int mysqlPort = 3306;
        String password = "sUpers3cret";
        String defaultBindAddress = "0.0.0.0";
        String bindAddress = Tests.Settings.getOpt("run_mysql.bindAddress").orElse(defaultBindAddress);
        ContainerParametry parametry = ContainerParametry.builder(getImageForMysqlTest())
                .bindPort(mysqlPort)
                .env("MYSQL_ROOT_PASSWORD", password)
                // entrypoint script supports just adding options as the command
                .blockingCommand("--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci",
                        "--bind-address=" + bindAddress)
                .build();
        List<String> colors = new ArrayList<>();
        try (DjContainerCreator runner = new DjContainerCreator(TestDockerManager.getInstance());
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                int hostPort = container.fetchPorts().stream()
                        .map(ContainerPort::hostBinding)
                        .filter(Objects::nonNull)
                        .mapToInt(FullSocketAddress::getPort)
                        .findFirst().orElseThrow(() -> new AssertionError("port not bound"));
                String jdbcUrl = "jdbc:mysql://127.0.0.1:" + hostPort + "/";
                System.out.println("connecting on " + jdbcUrl);
                String dbName = "widget_factory";
                String requiredLineSubstring = String.format("Server socket created on IP: '%s'.", bindAddress);
                System.out.format("awaiting line containing this substring: %s%n", requiredLineSubstring);
                Duration mysqlStartupTimeout = Tests.Settings.timeouts().get("run_mysql.startup", Duration.ofMinutes(5));
                AtomicBoolean doneWaiting = new AtomicBoolean(false);
                if (verboseWait) {
                    timer(Duration.ofSeconds(1), elapsed -> System.out.format("waited %s seconds for mysql up-ness%n", elapsed.getSeconds()), doneWaiting::get);
                }
                boolean logMessageAppeared = container.followStderr(BlockableLogFollower.untilLine(line -> line.contains(requiredLineSubstring), UTF_8, System.err))
                        .await(mysqlStartupTimeout);
                doneWaiting.set(true);
                System.out.format("saw line containing %s: %s%n", requiredLineSubstring, logMessageAppeared);
                try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, "root", password)) {
                    System.out.format("connected to %s%n", jdbcUrl);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA " + dbName);
                    }
                }
                jdbcUrl += dbName;
                Class.forName("org.mariadb.jdbc.Driver");
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

    @SuppressWarnings("UnusedReturnValue")
    private static Thread timer(Duration interval, Consumer<? super Duration> action, BooleanSupplier until) {
        Instant start = Instant.now();
        Thread thread = new Thread(() -> {
            while (!until.getAsBoolean()) {
                Duration elapsed = Duration.ofMillis(Instant.now().toEpochMilli() - start.toEpochMilli());
                action.accept(elapsed);
                try {
                    Thread.sleep(Durations.saturatedMilliseconds(interval));
                } catch (InterruptedException e) {
                    System.err.println("timer: " + e.toString());
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
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