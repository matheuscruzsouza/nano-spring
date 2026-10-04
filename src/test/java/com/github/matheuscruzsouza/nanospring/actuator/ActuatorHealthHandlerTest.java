package com.github.matheuscruzsouza.nanospring.actuator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActuatorHealthHandlerTest {

    private final Gson gson = new Gson();

    @Test
    public void testHealthResponseStructure() throws IOException {
        NanoHTTPD.Response response = ActuatorHealthHandler.handleHealth(null, null);
        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readResponseStream(response, out);
        String body = out.toString("UTF-8");

        JsonObject json = gson.fromJson(body, JsonObject.class);
        assertEquals("UP", json.get("status").getAsString());
        assertTrue(json.has("components"));

        JsonObject components = json.getAsJsonObject("components");
        assertTrue(components.has("memory"));
        assertTrue(components.has("network"));
        assertTrue(components.has("database"));

        JsonObject mem = components.getAsJsonObject("memory");
        assertEquals("UP", mem.get("status").getAsString());
        assertTrue(mem.getAsJsonObject("details").has("usedBytes"));
    }

    @Test
    public void testInfoResponseStructure() throws IOException {
        NanoHTTPD.Response response = ActuatorHealthHandler.handleInfo(null);
        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readResponseStream(response, out);
        String body = out.toString("UTF-8");

        JsonObject json = gson.fromJson(body, JsonObject.class);
        assertTrue(json.has("app"));
        JsonObject app = json.getAsJsonObject("app");
        assertEquals("nano-spring", app.get("name").getAsString());
        assertEquals("1.11.1", app.get("version").getAsString());
        assertTrue(app.has("activeProfiles"));
    }

    @Test
    public void testActuatorLogfileEndpoint() throws IOException {
        com.github.matheuscruzsouza.nanospring.logging.NanoLogger.info("TEST_LOG", "Hello enterprise log line 1");
        com.github.matheuscruzsouza.nanospring.logging.NanoLogger.warn("TEST_LOG", "Warning enterprise log line 2");

        NanoHTTPD.Response response = ActuatorHealthHandler.handleLogfile(null);
        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertTrue(response.getMimeType().contains("text/plain"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readResponseStream(response, out);
        String logs = out.toString("UTF-8");
        assertTrue(logs.contains("Hello enterprise log line 1"));
        assertTrue(logs.contains("Warning enterprise log line 2"));
    }

    private void readResponseStream(NanoHTTPD.Response response, ByteArrayOutputStream out) throws IOException {
        java.io.InputStream is = response.getData();
        if (is != null) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }
}
