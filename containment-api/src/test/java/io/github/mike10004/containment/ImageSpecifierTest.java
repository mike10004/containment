package io.github.mike10004.containment;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageSpecifierTest {

    @Test
    public void parseSpecifier() {
        Map<String, ImageSpecifier> testCases = new LinkedHashMap<>();
        testCases.put("x",  ImageSpecifier.fromNameOnly("x"));
        testCases.put("x:y",  ImageSpecifier.fromNameAndTag("x", "y"));
        testCases.put("z/x:y", ImageSpecifier.standard("x", "y", "z", null));
        testCases.put("localhost:5000/fedora/httpd:version1.0", ImageSpecifier.standard("httpd", "version1.0", "fedora", "localhost:5000"));
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
        Object[][] testCases = {
                {ImageSpecifier.standard("httpd", null, null, null), "httpd"},
                {ImageSpecifier.standard("httpd", null, "fedora", null), "fedora/httpd"},
                {ImageSpecifier.standard("httpd", "2.4", "fedora", null), "fedora/httpd:2.4"},
                {ImageSpecifier.standard("httpd", "2.4", "fedora", "localhost:5000"), "localhost:5000/fedora/httpd:2.4"},
                {ImageSpecifier.standard("httpd", null, "fedora", "localhost:5000"), "localhost:5000/fedora/httpd"},
                {ImageSpecifier.standard("httpd", null, null, "localhost:5000"), "localhost:5000/library/httpd"},
                {ImageSpecifier.standard("foo", "bar", null, "localhost:5000"), "localhost:5000/library/foo:bar"},
        };
        for (Object[] testCase : testCases) {
            ImageSpecifier input = (ImageSpecifier) testCase[0];
            String expected = (String) testCase[1];
            assertEquals("toString on " + input.describe(), expected, input.toString());
        }
    }

    @Test
    public void parseDigest() {
        DigestImageSpecifier.Digest expected = new DigestImageSpecifier.Digest("sha256", "45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2");
        assertEquals(expected, DigestImageSpecifier.Digest.parseDigest("sha256:45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2"));
    }

    @Test
    public void parseDigestImageSpecifier() {
        String token = "ubuntu@sha256:45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2";
        ImageSpecifier s = ImageSpecifier.parseSpecifier(token);
        assertTrue("is DigestImageSpecifier", s instanceof DigestImageSpecifier);
        assertEquals(new DigestImageSpecifier.Digest("sha256", "45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2"), s.pin());
    }
}