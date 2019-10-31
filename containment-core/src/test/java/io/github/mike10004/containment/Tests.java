package io.github.mike10004.containment;

import com.google.common.io.CharSource;
import io.github.mike10004.nitsick.SettingSet;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class Tests {

    private Tests() {}

    public static final String SETTINGS_DOMAIN = "containment-core.tests";

    public static final SettingSet Settings = SettingSet.global(SETTINGS_DOMAIN);

    public static ImageSpecifier getImageForEchoTest() {
        return getImageForTest("echoTest.image", "busybox:latest");
    }

    public static ImageSpecifier getImageForHttpdTest() {
        return getImageForTest("httpdTest.image", "httpd:2.4");
    }

    public static ImageSpecifier getImageForPrintenvTest() {
        return getImageForTest("printenvTest.image", "alpine:3.10.3");
    }

    public static ImageSpecifier getImageForTest(String identifier, String defaultValue) {
        String value = Settings.get(identifier);
        if (value == null) {
            value = defaultValue;
        }
        return ImageSpecifier.parseSpecifier(value);
    }

    public static void assertStdoutHasLine(DockerSubprocessResult<String> result, String requiredLine) throws IOException {
        boolean varWasSet = CharSource.wrap(result.stdout()).readLines().stream().anyMatch(requiredLine::equals);
        if (!varWasSet) {
            System.out.println(result.stdout());
        }
        assertTrue("result content", varWasSet);
    }

}
