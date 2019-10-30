package io.github.mike10004.containment;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class FullSocketAddressTest {

    @Test
    public void toUri() {
        FullSocketAddress a = FullSocketAddress.define("1.1.1.1", 1111);
        assertEquals("1.1.1.1", a.getHost());
        assertEquals(1111, a.getPort());
        assertEquals("1.1.1.1:1111", a.toString());
    }
}