package io.github.mike10004.containment;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class PollerTest {

    @Test
    public void poll_immediatelyTrue() throws Exception {
        testPoller(0, 0, 1000, Poller.StopReason.TIMEOUT, 0);
    }

    @Test
    public void poll_trueAfterZero() throws Exception {
        testPoller(0, 100, 1000, Poller.StopReason.RESOLVED, 0);
    }

    @Test
    public void poll_trueAfterOne() throws Exception {
        testPoller(1, 100, 1000, Poller.StopReason.RESOLVED, 1000);
    }

    @Test
    public void poll_notTrueBeforeLimit() throws Exception {
        testPoller(5, 4, 1000, Poller.StopReason.TIMEOUT, 4000);
    }

    @Test
    public void poll_abortFromCheck_0() throws Exception {
        poll_abortFromCheck(0);
    }

    @Test
    public void poll_abortFromCheck_1() throws Exception {
        poll_abortFromCheck(1);
    }

    @Test
    public void poll_abortFromCheck_2() throws Exception {
        poll_abortFromCheck(2);
    }

    public void poll_abortFromCheck(final int attempts) throws Exception {
        TestSleeper sleeper = new TestSleeper();
        Poller.PollOutcome<?> outcome = new Poller<Void>(sleeper) {
            @Override
            protected PollAnswer<Void> check(int pollAttemptsSoFar) {
                return pollAttemptsSoFar >= attempts ? abortPolling(null) : continuePolling();
            }
        }.poll(Duration.ofMillis(1000), Integer.MAX_VALUE); // poll forever
        Assert.assertEquals("reason", Poller.StopReason.ABORTED, outcome.reason);
        assertEquals("duration", attempts * 1000, sleeper.getDuration());
        assertEquals("sleep count", attempts, sleeper.getCount());
    }

    @Test
    public void poll_timeoutOverridesAbort_0() throws Exception {
        poll_timeoutOverridesAbort(0);
    }

    @Test
    public void poll_timeoutOverridesAbort_1() throws Exception {
        poll_timeoutOverridesAbort(1);
    }

    public void poll_timeoutOverridesAbort(final int attempts) throws Exception {
        TestSleeper sleeper = new TestSleeper();
        Poller.PollOutcome<?> outcome = new Poller<Void>(sleeper) {
            @Override
            protected PollAnswer<Void> check(int pollAttemptsSoFar) {
                return pollAttemptsSoFar >= attempts ? abortPolling(null) : continuePolling();
            }
        }.poll(Duration.ofMillis(1000), attempts); // poll forever
        Assert.assertEquals("reason", Poller.StopReason.TIMEOUT, outcome.reason);
        assertEquals("duration", attempts * 1000, sleeper.getDuration());
        assertEquals("sleep count", attempts, sleeper.getCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void poll_badArgs() throws Exception {
        testPoller(5, 4, -1000, null, 0);
    }

    @Test
    public void testDoNotSleepIfAboutToTimeOut_1() throws Exception {
        testDoNotSleepIfAboutToTimeOut(1000, 1, Poller.StopReason.TIMEOUT, 1000, 1);
    }

    @Test
    public void testDoNotSleepIfAboutToTimeOut_2() throws Exception {
        testDoNotSleepIfAboutToTimeOut(1000, 2, Poller.StopReason.TIMEOUT, 2000, 2);
    }

    public void testDoNotSleepIfAboutToTimeOut(long intervalMs, int maxPolls, Poller.StopReason stopReason, long expectedDuration, int expectedSleeps) throws Exception {
        TestSleeper sleeper = new TestSleeper();
        Poller.PollOutcome<Void> outcome = new Poller.SimplePoller(sleeper, () -> false).poll(Duration.ofMillis(intervalMs), maxPolls);
        assertEquals("stopReason", stopReason, outcome.reason);
        assertEquals("duration", expectedDuration, sleeper.getDuration());
        assertEquals("sleep count", expectedSleeps, sleeper.getCount());
    }

    private void testPoller(int returnTrueAfterNAttempts, int maxPollAttempts, long interval, Poller.StopReason expectedFinishReason, long expectedDuration) throws InterruptedException {
        TestSleeper sleeper = new TestSleeper();
        TestPoller poller = new TestPoller(sleeper, returnTrueAfterNAttempts);
        Poller.PollOutcome<?> evaluation = poller.poll(Duration.ofMillis(interval), maxPollAttempts);
        assertEquals("evaluation.result", expectedFinishReason, evaluation.reason);
        assertEquals("duration", expectedDuration, sleeper.getDuration());
    }

    private static class TestPoller extends Poller<Long> {

        private final AtomicLong values = new AtomicLong(0L);
        private final int returnTrueAfterNAttempts;

        public TestPoller(TestSleeper sleeper, int returnTrueAfterNAttempts) {
            super(sleeper);
            this.returnTrueAfterNAttempts = returnTrueAfterNAttempts;
        }

        @Override
        protected PollAnswer<Long> check(int pollAttemptsSoFar) {
            return pollAttemptsSoFar >= returnTrueAfterNAttempts
                    ? resolve(values.incrementAndGet())
                    : continuePolling();
        }
    }

    private static class TestSleeper implements Poller.Sleeper {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final AtomicLong totalDuration = new AtomicLong(0L);

        public long getDuration() {
            return totalDuration.get();
        }

        public int getCount() {
            return counter.get();
        }

        @Override
        public void sleep(Duration duration) throws InterruptedException {
            totalDuration.addAndGet(duration.toMillis());
            counter.incrementAndGet();
        }
    }

}