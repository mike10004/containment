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
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance());
             RunnableContainer runnable = runner.create(parametry)) {
            try (RunningContainer container = runnable.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), Collections.emptyMap(), UTF_8);
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
        public void perform(ContainerInfo unstartedContainer) {
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
        DockerClient client = TestDockerManager.getInstance().openClient();
        String copiedFileDestDir = "/root/";
        String pathnameOfFileInContainer = copiedFileDestDir + file.getName();
        PreCopier copier = new PreCopier(client, file, copiedFileDestDir);
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance());
            RunnableContainer runnableContainer = runner.create(parametry)) {
            runnableContainer.execute(copier);
            try (RunningContainer container = runnableContainer.start()) {
                DockerExecutor executor = new DockerExecExecutor(container.info().id(), Collections.emptyMap(), UTF_8);
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
        try (ContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance());
             RunnableContainer runnable = runner.create(parametry)) {
            try (RunningContainer container = runnable.start()) {
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

    private static ImageSpecifier getImageForMysqlTest() {
        return Tests.getImageForTest("run_mysql.image", "mariadb:10.4");
    }

    @SuppressWarnings("SqlDialectInspection")
    @Test
    public void run_mysql() throws Exception {
        boolean verboseWait = Tests.Settings.get("run_mysql.verboseWait", false);
        int mysqlPort = 3306;
        String password = "sUpers3cret";
        // The default bind address here was obtained by observation; future updates to the
        // container image or to the docker engine may affect this value. It can be ascertained
        // by `docker inspect` (NetworkSettings.Networks.bridge.IPAddress), but that must be
        // executed on a running container, and we need this value at time of container creation.
        // If this test starts failing, try it with bind address 0.0.0.0, and if that works,
        // it probably means this value needs to be changed.
        String defaultBindAddress = "172.17.0.2";
        String bindAddress = Tests.Settings.getOpt("run_mysql.bindAddress").orElse(defaultBindAddress);
        ContainerParametry parametry = ContainerParametry.builder(getImageForMysqlTest())
                .expose(mysqlPort)
                .env("MYSQL_ROOT_PASSWORD", password)
                // entrypoint script supports just adding options as the command
                .command("--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci",
                        "--bind-address=" + bindAddress)
                .build();
        List<String> colors = new ArrayList<>();
        try (DjContainerRunner runner = new DjContainerRunner(TestDockerManager.getInstance());
             RunnableContainer runnable = runner.create(parametry)) {
            try (RunningContainer container = runnable.start()) {
                int hostPort = container.fetchPorts().stream()
                        .filter(PortMapping::isBound)
                        .map(pm -> pm.host)
                        .filter(Objects::nonNull)
                        .mapToInt(FullSocketAddress::getPort)
                        .findFirst().orElseThrow(() -> new AssertionError("port not bound"));
                String jdbcUrl = "jdbc:mysql://127.0.0.1:" + hostPort + "/";
                System.out.println("connecting on " + jdbcUrl);
                String dbName = "widget_factory";
                String requiredLineRegex = "^.*Server socket created on IP: '\\d+\\.\\d+\\.\\d+\\.\\d+'\\.$";
                System.out.format("awaiting line matching in %s%n", requiredLineRegex);
                Duration mysqlStartupTimeout = Tests.Settings.timeouts().get("run_mysql.startup", Duration.ofMinutes(5));
                AtomicBoolean doneWaiting = new AtomicBoolean(false);
                if (verboseWait) {
                    timer(Duration.ofSeconds(1), elapsed -> System.out.format("waited %s seconds for mysql up-ness%n", elapsed.getSeconds()), doneWaiting::get);
                }
                boolean logMessageAppeared = container.followStderr(BlockableLogFollower.untilLine(line -> line.matches(requiredLineRegex), UTF_8, System.err))
                        .await(mysqlStartupTimeout);
                doneWaiting.set(true);
                System.out.format("saw line matching %s: %s%n", requiredLineRegex, logMessageAppeared);
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