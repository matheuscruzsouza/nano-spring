package com.github.matheuscruzsouza.nanospring.server;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.Properties;

public class EnvironmentTest {

    @Before
    public void setup() {
        Environment.reset();
    }

    @After
    public void teardown() {
        Environment.reset();
    }

    @Test
    public void testSetAndGetProperty() {
        Environment.setProperty("test.key", "test.value");
        assertEquals("test.value", Environment.getProperty("test.key"));
    }

    @Test
    public void testDefaultValue() {
        assertEquals("default", Environment.getProperty("non.existent", "default"));
    }

    @Test
    public void testActiveProfiles() {
        Environment.setActiveProfiles("dev", "test");
        assertTrue(Environment.hasActiveProfile("dev"));
        assertTrue(Environment.hasActiveProfile("test"));
        assertFalse(Environment.hasActiveProfile("prod"));
    }
}
