package io.github.mike10004.containment.subprocess;

import io.github.mike10004.containment.ContainerPort;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class PsOutputParserTest {

    @Test
    public void parsePortsContent() throws Exception {
        Map<String, List<ContainerPort>> testCases = new LinkedHashMap<>();
        testCases.put("80/tcp -> 0.0.0.0:32768", Arrays.asList(ContainerPort.bound(80, "tcp", 32768, "0.0.0.0")));
        testCases.put("80/tcp", Arrays.asList(ContainerPort.unbound( 80, "tcp")));
        testCases.put("0.0.0.0:32771->80/tcp, 0.0.0.0:32770->443/tcp", Arrays.asList(ContainerPort.bound(80, "tcp", 32771, "0.0.0.0"), ContainerPort.bound(443, "tcp", 32770, "0.0.0.0")));
        testCases.forEach((input, expected) -> {
            List<ContainerPort> actual  = PsOutputParser.parsePortsContent(input);
            assertEquals("from input " + input, expected, actual);
        });
    }
}