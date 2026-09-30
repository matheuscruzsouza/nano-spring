package com.github.matheuscruzsouza.nanospring.cors;

import org.junit.Test;
import fi.iki.elonen.NanoHTTPD;

import java.util.Properties;

import static org.junit.Assert.*;

public class CorsConfigurationTest {

    @Test
    public void testDefaults() {
        CorsConfiguration config = new CorsConfiguration(null);
        assertTrue(config.isEnabled());
        assertEquals("*", config.getAllowedOrigins());
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", config.getAllowedMethods());
        assertEquals("Content-Type,Authorization,X-Requested-With,Accept", config.getAllowedHeaders());
        assertEquals("86400", config.getMaxAge());
    }

    @Test
    public void testCustomProperties() {
        Properties props = new Properties();
        props.setProperty(CorsConfiguration.KEY_ENABLED, "false");
        props.setProperty(CorsConfiguration.KEY_ORIGINS, "http://meuhub.local");
        props.setProperty(CorsConfiguration.KEY_METHODS, "GET,POST");
        props.setProperty(CorsConfiguration.KEY_HEADERS, "Authorization,Content-Type");
        props.setProperty(CorsConfiguration.KEY_MAX_AGE, "3600");

        CorsConfiguration config = new CorsConfiguration(props);
        assertFalse(config.isEnabled());
        assertEquals("http://meuhub.local", config.getAllowedOrigins());
        assertEquals("GET,POST", config.getAllowedMethods());
        assertEquals("Authorization,Content-Type", config.getAllowedHeaders());
        assertEquals("3600", config.getMaxAge());
    }

    @Test
    public void testApplyHeadersToResponse() {
        CorsConfiguration config = new CorsConfiguration();
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", "Hello");

        response.addHeader("Access-Control-Allow-Origin", config.getAllowedOrigins());
        response.addHeader("Access-Control-Allow-Methods", config.getAllowedMethods());
        response.addHeader("Access-Control-Allow-Headers", config.getAllowedHeaders());
        response.addHeader("Access-Control-Max-Age", config.getMaxAge());

        assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", response.getHeader("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization,X-Requested-With,Accept", response.getHeader("Access-Control-Allow-Headers"));
        assertEquals("86400", response.getHeader("Access-Control-Max-Age"));
    }
}
