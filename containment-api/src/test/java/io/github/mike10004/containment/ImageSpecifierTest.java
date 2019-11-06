package io.github.mike10004.containment;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ImageSpecifierTest {

    @Test
    public void parseSpecifier() {
        Map<String, ImageSpecifier> testCases = new LinkedHashMap<>();
        testCases.put("x",  ImageSpecifier.fromNameOnly("x"));
        testCases.put("x:y",  ImageSpecifier.fromNameAndTag("x", "y"));
        testCases.put("z/x:y", new ImageSpecifier("x", "y", "z", null));
        testCases.put("localhost:5000/fedora/httpd:version1.0", new ImageSpecifier("httpd", "version1.0", "fedora", "localhost:5000"));
        testCases.forEach((token, expected) -> {
            ImageSpecifier actual = ImageSpecifier.parseSpecifier(token);
            assertEquals(token, expected, actual);
        });
    }

    @Test
    public void withDefaultTag() {
        assertEquals("add tag", "x:y", ImageSpecifier.fromNameAndTag("x", null).withDefaultTag("y").toString());
        assertEquals("do not add", "x:z",  ImageSpecifier.fromNameAndTag("x", "z").withDefaultTag("y").toString());
    }

    @Test
    public void test_toString() {
        String token = "fedora/httpd:2.4";
        ImageSpecifier localImageSpec = ImageSpecifier.parseSpecifier(token);
        assertEquals("toString", token, localImageSpec.toString());

        ImageSpecifier repoless = new ImageSpecifier("foo", "bar", null, "localhost:5000");
        assertEquals("toString", "localhost:5000/library/foo:bar", repoless.toString());
    }
}