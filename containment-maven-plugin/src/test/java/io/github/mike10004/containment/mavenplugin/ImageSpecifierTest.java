package io.github.mike10004.containment.mavenplugin;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageSpecifierTest {

    @Test
    public void parseSpecifier() {

        Map<String, ImageSpecifier> testCases = new LinkedHashMap<>();
        testCases.put("x", new ImageSpecifier("x"));
        testCases.put("x:y", new ImageSpecifier("x", "y"));
        testCases.put("z/x:y", new ImageSpecifier("x", "y", "z", null));
        testCases.put("localhost:5000/fedora/httpd:version1.0", new ImageSpecifier("httpd", "version1.0", "fedora", "localhost:5000"));
        testCases.forEach((token, expected) -> {
            ImageSpecifier actual = ImageSpecifier.parseSpecifier(token);
            assertEquals(token, expected, actual);
        });
    }

    @Test
    public void withDefaultTag() {
        assertEquals("add tag", "x:y", new ImageSpecifier("x", null).withDefaultTag("y").toString());
        assertEquals("do not add", "x:z", new ImageSpecifier("x", "z").withDefaultTag("y").toString());
    }
}