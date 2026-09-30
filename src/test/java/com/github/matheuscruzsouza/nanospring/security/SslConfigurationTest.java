package com.github.matheuscruzsouza.nanospring.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SslConfigurationTest {

    @Test
    public void testDefaultSslConfigurationDisabled() {
        SslConfiguration config = new SslConfiguration();
        assertFalse(config.isEnabled());
        assertNull(config.createSslSocketFactory(null));
        assertEquals("PKCS12", config.getKeyStoreType());
    }

    @Test
    public void testCustomSslConfiguration() {
        SslConfiguration config = new SslConfiguration(
                true,
                "certificates/server.p12",
                "secret123",
                "BKS",
                "TLSv1.2,TLSv1.3"
        );

        assertTrue(config.isEnabled());
        assertEquals("certificates/server.p12", config.getKeyStorePath());
        assertEquals("BKS", config.getKeyStoreType());
        assertNotNull(config.getProtocols());
        assertEquals(2, config.getProtocols().length);
        assertEquals("TLSv1.2", config.getProtocols()[0]);
        assertEquals("TLSv1.3", config.getProtocols()[1]);
    }
}
