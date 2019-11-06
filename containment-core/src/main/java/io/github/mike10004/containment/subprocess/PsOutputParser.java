package io.github.mike10004.containment.subprocess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import io.github.mike10004.containment.ContainerPort;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class PsOutputParser implements DockerPsContent {
    private final String psOutput;

    public PsOutputParser(String psOutput) {
        this.psOutput = psOutput;
    }

    @Override
    public List<ContainerPort> parsePortMappings() {
        try {
            JsonNode root = new ObjectMapper().readTree(psOutput);
            String portsContent = root.get("Ports").asText();
            return parsePortsContent(portsContent);
        } catch (IOException e) {
            throw new RuntimeException("json parse exception", e);
        }
    }

    static List<ContainerPort> parsePortsContent(String portsContent) {
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
                        return ContainerPort.bound(containerPort, containerProtocol, hap.getPort(), hap.getHost());
                    } else {
                        return ContainerPort.unbound(containerPort, containerProtocol);
                    }
                }).collect(Collectors.toList());
    }

}
