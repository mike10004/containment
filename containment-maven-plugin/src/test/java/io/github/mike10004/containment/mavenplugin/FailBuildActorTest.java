package io.github.mike10004.containment.mavenplugin;

import org.junit.Test;

public class FailBuildActorTest {

    @Test(expected = FailBuildActor.RequiredImageAbsentException.class)
    public void perform() throws Exception {
        new FailBuildActor(new LogBucket()).perform(RequireImageParametry.newBuilder("asdf").build(), null);
    }
}