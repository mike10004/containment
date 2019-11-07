package io.github.mike10004.containment.core;

import com.google.common.io.CharSource;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.Durations;
import io.github.mike10004.containment.ImageSpecifier;
import io.github.mike10004.nitsick.SettingSet;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class Tests {

    private Tests() {}

    public static final String SETTINGS_DOMAIN = "containment-core.tests";

    public static final SettingSet Settings = SettingSet.system(SETTINGS_DOMAIN);

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

    public static void assertStdoutHasLine(ContainerSubprocessResult<String> result, String requiredLine) throws IOException {
        boolean varWasSet = CharSource.wrap(result.stdout()).readLines().stream().anyMatch(requiredLine::equals);
        if (!varWasSet) {
            System.out.println(result.stdout());
        }
        assertTrue("result content", varWasSet);
    }

    /**
     * Invokes an action on a separate thread at fixed intervals until a condition is satisfied.
     * @param interval the interval
     * @param action the action
     * @param until the condition
     * @return an already-started daemon thread
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Thread startTimer(Duration interval, Consumer<? super Duration> action, BooleanSupplier until) {
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

}
