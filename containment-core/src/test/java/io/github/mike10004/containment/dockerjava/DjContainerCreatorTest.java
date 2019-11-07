package io.github.mike10004.containment.dockerjava;

import com.google.common.io.ByteSource;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerExecutor;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerPort;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.FullSocketAddress;
import io.github.mike10004.containment.ImageSpecifier;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.core.DjManagedTestBase;
import io.github.mike10004.containment.core.Tests;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DjContainerCreatorTest extends DjManagedTestBase  {

    @ClassRule
    public static final TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void execute_setContainerEnvironmentVariables() throws Exception {
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForPrintenvTest())
                .env("FOO", "bar")
                .commandToWaitIndefinitely()
                .build();

        ContainerSubprocessResult<String> result;
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                ContainerExecutor executor = container.executor();
                result = executor.execute(UTF_8, "printenv");
            }
        }
        assertEquals("process exit code", 0, result.exitCode());
        Tests.assertStdoutHasLine(result, "FOO=bar");
    }

    @Test
    public void run_exposePorts() throws Exception {
        int httpdPort = 80;
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForHttpdTest())
                .bindPort(httpdPort)
                .build();
        String result;
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                List<ContainerPort> ports = container.fetchPorts();
                ContainerPort httpdExposedPortMapping = ports.stream().filter(p -> p.number() == httpdPort).findFirst().orElseThrow(() -> new IllegalStateException("no mapping for port 80 found"));
                assertTrue("exposed", httpdExposedPortMapping.isBound());
                FullSocketAddress hostBinding = httpdExposedPortMapping.hostBinding();
                assertNotNull(hostBinding);
                result = fetchPageContent(hostBinding.getPort());
            }
        }
        System.out.println(result);
        assertEquals("page text", HTTPD_DEFAULT_PAGE_CONTENT, result.trim());
    }

    private String fetchPageContent(int port) throws IOException {
        URL url = new URL("http", "localhost", port, "/");
        byte[] content = new JreClient().fetchPageContent(url);
        return new String(content, UTF_8);
    }

    @Test
    public void run_exposePorts_predefinedHostPort() throws Exception {
        int containerPort = 80, hostPort = acquirePortForPredefinedHostPortTest();
        ContainerParametry parametry = ContainerParametry.builder(Tests.getImageForHttpdTest())
                .bindPort(containerPort, hostPort)
                .build();
        String result;
        try (ContainerCreator runner = new DjContainerCreator(dockerManager);
             StartableContainer runnable = runner.create(parametry)) {
            try (StartedContainer container = runnable.start()) {
                // it's not certain that httpd is ready to accept connections immediately, so we sleep a little here
                // TODO maybe wait for httpd output line ending in something like "[core:notice] [pid 1:tid 140486139090048] AH00094: Command line: 'httpd -D FOREGROUND'"
                Thread.sleep(500);
                result = fetchPageContent(hostPort);
                List<ContainerPort> ports = container.fetchPorts();
                assertEquals("num ports", 1, ports.size());
                FullSocketAddress hostBinding = ports.get(0).hostBinding();
                assertNotNull(hostBinding);
                assertEquals(hostPort, hostBinding.getPort());
            }
        }
        System.out.println(result);
        assertEquals("page text", HTTPD_DEFAULT_PAGE_CONTENT, result.trim());
    }

    private static final String HTTPD_DEFAULT_PAGE_CONTENT = "<html><body><h1>It works!</h1></body></html>";

    private static int acquirePortForPredefinedHostPortTest() throws IOException {
        String portStr = System.getProperty("containment.tests.predefinedHostPort", "").trim();
        System.out.format("using reserved port if nonempty: \"%s\"%n", portStr);
        if (!portStr.isEmpty()) {
            return Integer.parseInt(portStr);
        }
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
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
        try (DjContainerCreator runner = new DjContainerCreator(dockerManager);
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
                    Tests.startTimer(Duration.ofSeconds(1), elapsed -> System.out.format("waited %s seconds for mysql up-ness%n", elapsed.getSeconds()), doneWaiting::get);
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