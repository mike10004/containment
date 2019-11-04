package io.github.mike10004.containment.dockerjava;

import io.github.mike10004.containment.dockerjava.BlockableLogFollower;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class BlockableLogFollowerTest {

    @Test
    public void untilLine() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        Charset charset = UTF_8;
        PrintStream echo = new PrintStream(baos, true, charset.name());
        BlockableLogFollower f = BlockableLogFollower.untilLine("hello"::equals, charset, echo);
        f.accept("foo\n".getBytes(charset));
        f.accept("hello\n".getBytes(charset));
        f.accept("bar\n".getBytes(charset));
        assertEquals("remaining", 0, f.remaining());
        echo.flush();
        String content = new String(baos.toByteArray(), charset);
        assertEquals("content", "foo\nhello\nbar\n", content);
        assertTrue(f.await(Duration.ZERO));
    }
}