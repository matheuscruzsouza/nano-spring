package com.github.matheuscruzsouza.nanospring.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NanoSpringServiceTest {

    private static class DummyNanoService extends NanoSpringService {
        @Override
        protected String getBasePackage() {
            return "com.test.app";
        }
    }

    @Test
    public void testDefaultServiceProperties() {
        DummyNanoService service = new DummyNanoService();
        assertEquals("com.test.app", service.getBasePackage());
        assertEquals(8080, service.getPort());
        assertEquals(NanoSpringService.DEFAULT_CHANNEL_ID, service.getChannelId());
        assertEquals(NanoSpringService.DEFAULT_CHANNEL_NAME, service.getChannelName());
        assertEquals(NanoSpringService.DEFAULT_NOTIFICATION_ID, service.getNotificationId());
        assertEquals("Nano-Spring HTTP Server", service.getNotificationTitle());
        assertEquals("Servidor rodando na porta 8080", service.getNotificationText());
        assertTrue(service.isWakeLockEnabled());
        assertTrue(service.isMulticastLockEnabled());
        assertNull(service.onBind(null));
        assertNull(service.getServer());
    }
}
