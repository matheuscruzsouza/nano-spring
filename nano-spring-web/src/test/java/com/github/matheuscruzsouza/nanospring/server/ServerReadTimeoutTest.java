package com.github.matheuscruzsouza.nanospring.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerReadTimeoutTest {

    @Before
    @After
    public void cleanup() {
        Environment.reset();
    }

    @Test
    public void testDefaultReadTimeout() {
        int timeout = Integer.parseInt(Environment.getProperty("nano.server.read-timeout", "5000"));
        assertEquals(5000, timeout);
    }

    @Test
    public void testCustomReadTimeout() {
        Environment.setProperty("nano.server.read-timeout", "15000");
        int timeout = Integer.parseInt(Environment.getProperty("nano.server.read-timeout", "5000"));
        assertEquals(15000, timeout);
    }
}
