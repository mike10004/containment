package io.github.mike10004.containment.mavenplugin;

import org.junit.Test;

import static org.junit.Assert.*;

public class IgnoreImageActorTest {

    @Test
    public void perform() {
        LogBucket log = new LogBucket();
        new IgnoreImageActor(log).perform(RequireImageParametry.newBuilder("asdf").build(), null);
        LogBucket.LogEntry logEntry = log.getEntries().get(0);
        assertEquals("message", "ignoring absence of 'asdf' as directed", logEntry.message);
    }
}