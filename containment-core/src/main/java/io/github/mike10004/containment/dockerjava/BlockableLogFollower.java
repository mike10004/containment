package io.github.mike10004.containment.dockerjava;

import com.google.common.primitives.Ints;
import io.github.mike10004.containment.Durations;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class BlockableLogFollower implements Consumer<byte[]> {

    private final Predicate<? super byte[]> examiner;

    private final CountDownLatch latch;

    @Nullable
    private final PrintStream echo;

    public BlockableLogFollower(Predicate<? super byte[]> examiner) {
        this(examiner, null);
    }

    public BlockableLogFollower(Predicate<? super byte[]> examiner, @Nullable PrintStream echo) {
        this(examiner, 1, echo);
    }

    public BlockableLogFollower(Predicate<? super byte[]> examiner, int numSuccessesForSignal, @Nullable PrintStream echo) {
        this.examiner = requireNonNull(examiner);
        latch = new CountDownLatch(numSuccessesForSignal);
        this.echo = echo;
    }

    public int remaining() {
        return Ints.checkedCast(latch.getCount());
    }

    @Override
    public void accept(byte[] bytes) {
        if (echo != null) {
            try {
                echo.write(bytes);
                echo.flush();
            } catch (IOException ignore) {
            }
        }
        if (examiner.test(bytes)) {
            latch.countDown();
        }
    }

    public boolean await(Duration timeout) throws InterruptedException {
        long millis = Durations.saturatedMilliseconds(timeout);
        return latch.await(millis, TimeUnit.MILLISECONDS);
    }

    public static BlockableLogFollower untilLine(Predicate<? super String> singleLinePredicate, Charset charset) {
        return untilLine(singleLinePredicate, charset, null);
    }

    public static BlockableLogFollower untilLine(Predicate<? super String> singleLinePredicate, Charset charset, @Nullable PrintStream echo) {
        return new BlockableLogFollower(singleLinePredicate(charset, singleLinePredicate), echo);
    }

    /**
     * Creates a single line predicate that ignores encoding exceptions.
     * @param charset
     * @param evaluator
     * @return
     */
    public static Predicate<byte[]> singleLinePredicate(Charset charset, Predicate<? super String> evaluator) {
        return singleLinePredicate(charset, evaluator, ignore -> {});
    }

    public static Predicate<byte[]> singleLinePredicate(Charset charset, Predicate<? super String> evaluator, Consumer<? super IOException> encodingExceptionListener) {
        return new Predicate<byte[]>() {
            @Override
            public boolean test(byte[] bytes) {
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (evaluator.test(line)) {
                            return true;
                        }
                    }
                } catch (IOException e) {
                    encodingExceptionListener.accept(e);
                }
                return false;
            }
        };
    }
}
