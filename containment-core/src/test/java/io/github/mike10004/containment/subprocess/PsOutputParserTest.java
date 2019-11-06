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
        testCases.put("80/tcp -> 0.0.0.0:32768", Arrays.asList(new ContainerPort(32768, "0.0.0.0", 80, "tcp")));
        testCases.put("80/tcp", Arrays.asList(new ContainerPort( 80, "tcp")));
        testCases.put("0.0.0.0:32771->80/tcp, 0.0.0.0:32770->443/tcp", Arrays.asList(new ContainerPort(32771, "0.0.0.0", 80, "tcp"), new ContainerPort(32770, "0.0.0.0", 443, "tcp")));
        testCases.forEach((input, expected) -> {
            List<ContainerPort> actual  = PsOutputParser.parsePortsContent(input);
            assertEquals("from input " + input, expected, actual);
        });
    }
}