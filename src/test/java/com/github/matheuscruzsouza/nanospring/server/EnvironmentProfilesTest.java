package com.github.matheuscruzsouza.nanospring.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class EnvironmentProfilesTest {

    @Before
    @After
    public void cleanup() {
        Environment.reset();
    }

    @Test
    public void testDefaultProperties() {
        Environment.setProperty("server.port", "8080");
        Environment.setProperty("app.name", "DefaultApp");

        assertEquals("8080", Environment.getProperty("server.port"));
        assertEquals("DefaultApp", Environment.getProperty("app.name"));
        assertEquals("fallback", Environment.getProperty("non.existing", "fallback"));
    }

    @Test
    public void testActiveProfilesSettingAndQuerying() {
        Environment.setActiveProfiles("dev", "pos");

        String[] profiles = Environment.getActiveProfiles();
        assertEquals(2, profiles.length);
        assertEquals("dev", profiles[0]);
        assertEquals("pos", profiles[1]);

        assertTrue(Environment.hasActiveProfile("dev"));
        assertTrue(Environment.hasActiveProfile("pos"));
        assertFalse(Environment.hasActiveProfile("prod"));
    }

    @Test
    public void testProfilePropertyOverride() {
        // Default properties
        Properties baseProps = new Properties();
        baseProps.setProperty("server.port", "8080");
        baseProps.setProperty("datasource.url", "jdbc:sqlite:default.db");
        Environment.loadProperties(baseProps);

        assertEquals("8080", Environment.getProperty("server.port"));
        assertEquals("jdbc:sqlite:default.db", Environment.getProperty("datasource.url"));

        // Profile override (simulating loading application-dev.properties)
        Properties devProps = new Properties();
        devProps.setProperty("datasource.url", "jdbc:sqlite:dev.db");
        devProps.setProperty("nano.logging.level", "DEBUG");
        Environment.loadProperties(devProps);

        assertEquals("8080", Environment.getProperty("server.port")); // Unchanged base
        assertEquals("jdbc:sqlite:dev.db", Environment.getProperty("datasource.url")); // Overridden
        assertEquals("DEBUG", Environment.getProperty("nano.logging.level")); // Added by profile
    }
}
