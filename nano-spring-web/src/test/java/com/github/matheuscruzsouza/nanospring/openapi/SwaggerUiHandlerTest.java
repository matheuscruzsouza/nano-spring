package com.github.matheuscruzsouza.nanospring.openapi;

import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.io.FileOutputStream;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.*;

public class SwaggerUiHandlerTest {

    @After
    public void tearDown() {
        Environment.reset();
    }

    @Test
    public void testHandleApiDocsResponse() throws Exception {
        NanoHTTPD.Response response = SwaggerUiHandler.handleApiDocs();
        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertEquals("application/json; charset=UTF-8", response.getMimeType());
        assertEquals("*", response.getHeader("access-control-allow-origin"));

        String output = readResponseString(response);
        assertTrue(output.contains("openapi"));
        assertTrue(output.contains("3.0.1"));
    }

    @Test
    public void testHandleUiResponse() throws Exception {
        NanoHTTPD.Response response = SwaggerUiHandler.handleUi();
        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
        assertEquals("text/html; charset=UTF-8", response.getMimeType());
        assertEquals("*", response.getHeader("access-control-allow-origin"));

        String output = readResponseString(response);
        assertTrue(output.contains("<!DOCTYPE html>"));
        assertTrue(output.contains("Nano-Swagger"));
        assertTrue(output.contains("/v3/api-docs"));
        assertTrue(output.contains("executeRequest"));
        assertTrue(output.contains("Try it out"));
        assertTrue(output.contains("background-color: #0b0f19"));
    }

    @Test
    public void testCustomCssInjection() throws Exception {
        File tempCss = File.createTempFile("custom-swagger", ".css");
        tempCss.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempCss)) {
            fos.write("body { background-color: #ff00ff; } /* custom-theme-applied */".getBytes("UTF-8"));
        }

        Environment.setProperty("nano.swagger.css-path", tempCss.getAbsolutePath());

        NanoHTTPD.Response response = SwaggerUiHandler.handleUi();
        String output = readResponseString(response);

        assertTrue(output.contains("custom-theme-applied"));
        assertTrue(output.contains("#ff00ff"));
        assertFalse(output.contains("background-color: #0b0f19"));
    }

    private String readResponseString(NanoHTTPD.Response response) throws java.io.IOException {
        java.io.InputStream is = response.getData();
        if (is == null) return "";
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toString("UTF-8");
    }
}
