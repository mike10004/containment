package io.github.mike10004.containment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.CharSource;
import com.google.common.net.HostAndPort;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.SubprocessException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class DjRunningContainer implements RunningContainer {

    private final DockerClient client;
    private final String containerId;
    private final LoadingCache<Datum, String> cache;

    public DjRunningContainer(DockerClient client, String containerId) {
        this.client = client;
        this.containerId = containerId;
        cache = CacheBuilder.newBuilder().build(new CacheLoader<Datum, String>() {
            @Override
            public String load(Datum key) throws ContainmentException {
                return execute(key);
            }
        });
    }

    private enum Datum {
        PS
    }

    @Override
    public String id() {
        return containerId;
    }

    @Override
    public List<PortMapping> fetchPorts() throws ContainmentException {
        try {
            JsonNode root = new ObjectMapper().readTree(cache.get(Datum.PS));
            String portsContent = root.get("Ports").asText();
            return parsePortsContent(portsContent);
        } catch (IOException e) {
            throw new ContainmentException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ContainmentException) {
                throw (ContainmentException) e.getCause();
            }
            throw new ContainmentException(e);
        }
    }

    private String execute(Datum d) throws ContainmentException {
        switch (d) {
            case PS:
                return executePs_(id());
            default:
                throw new IllegalArgumentException(String.valueOf(d));
        }
    }

    private static String executePs_(final String containerId) throws ContainmentException {
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
            ProcessMonitor<String, String> monitor = Subprocess.running("docker")
                    .arg("ps")
                    .arg("--filter=id=" + containerId)
                    .arg("--format={{json .}}")
                    .build()
                    .launcher(tracker)
                    .outputStrings(StandardCharsets.UTF_8)
                    .launch();
            ProcessResult<String, String> result = monitor.await();
            if (result.exitCode() != 0) {
                throw new ContainmentException("docker port: " + result);
            }
            List<String> lines = CharSource.wrap(result.content().stdout()).readLines();
            if (lines.isEmpty()) {
                throw new ContainmentException("container " + containerId + " not listed in output of `docker ps`" + result.toString());
            }
            if (lines.size() > 1) {
                throw new ContainmentException("expect exactly one line in  ps output, but got " + lines.size());
            }
            return lines.get(0);
        } catch (InterruptedException | SubprocessException | IOException e) {
            throw new ContainmentException(e);
        }
    }

    static List<PortMapping> parsePortsContent(String portsContent) {
        String[] tokens = portsContent.split(",\\s*");
        return Arrays.stream(tokens)
                .map(token -> {
                    List<String> parts = Splitter.on(Pattern.compile("\\s*->\\s*")).splitToList(token);
                    String hostPart = parts.stream().filter(s -> s.matches("^\\S+:\\d+$")).findFirst().orElse(null);
                    String containerPart = parts.stream().filter(s -> s.matches("^\\d+/\\w+$")).findFirst().orElse(null);
                    if (containerPart == null) {
                        throw new IllegalArgumentException("unexpected syntax in " + StringUtils.abbreviate(token, 128));
                    }
                    String[] containerParts = containerPart.split("/");
                    int containerPort = Integer.parseInt(containerParts[0]);
                    String containerProtocol = containerParts[1];
                    if (hostPart != null) {
                        HostAndPort hap = HostAndPort.fromString(hostPart);
                        return new PortMapping(hap.getPort(), hap.getHostText(), containerPort, containerProtocol);
                    } else {
                        return new PortMapping(containerPort, containerProtocol);
                    }
                }).collect(Collectors.toList());
    }

    @Override
    public void close() throws ContainmentException {
        try {
            try {
                client.stopContainerCmd(id())
                        .withTimeout(1)
                        .exec();
            } catch (NotFoundException e) {
                // probably means container was terminated through other means
            }
        } catch (com.github.dockerjava.api.exception.DockerException e) {
            throw new ContainmentException(e);
        }
    }
}
