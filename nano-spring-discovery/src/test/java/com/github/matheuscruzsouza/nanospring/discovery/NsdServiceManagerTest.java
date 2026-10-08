package com.github.matheuscruzsouza.nanospring.discovery;

import org.junit.Test;
import static org.junit.Assert.*;

public class NsdServiceManagerTest {

    @Test
    public void testDefaults() {
        assertEquals("nano-spring", NsdServiceManager.DEFAULT_SERVICE_NAME);
        assertEquals("_http._tcp.", NsdServiceManager.DEFAULT_SERVICE_TYPE);

        NsdServiceManager manager = new NsdServiceManager();
        assertFalse(manager.isRegistered());
        assertNull(manager.getRegisteredServiceName());
    }

    @Test
    public void testUnregisterWhenNotRegisteredDoesNotThrow() {
        NsdServiceManager manager = new NsdServiceManager();
        manager.unregister();
        assertFalse(manager.isRegistered());
    }
}
