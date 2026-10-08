package com.github.matheuscruzsouza.nanospring.handler;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.github.matheuscruzsouza.nanospring.server.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.*;

public class AsyncCompletableFutureTest {

    @RestController("/async")
    public static class AsyncController {

        @GetMethod("/simple")
        public CompletableFuture<String> simpleAsync() {
            return CompletableFuture.supplyAsync(() -> "Async Hello World");
        }

        @GetMethod("/entity")
        public CompletableFuture<ResponseEntity<Map<String, Object>>> entityAsync() {
            return CompletableFuture.supplyAsync(() -> {
                Map<String, Object> map = new HashMap<>();
                map.put("printed", true);
                map.put("jobId", "POS-9988");
                return ResponseEntity.status(HttpStatus.CREATED).body(map);
            });
        }

        @GetMethod("/timeout")
        public CompletableFuture<String> timeoutAsync() {
            // An incomplete future that will not complete before timeout
            return new CompletableFuture<>();
        }

        @GetMethod("/error")
        public CompletableFuture<String> errorAsync() {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("POS printer out of paper"));
            return failed;
        }
    }

    private AsyncController controller;

    @Before
    public void setUp() {
        Environment.reset();
        Environment.setProperty("nano.async.timeout-seconds", "1"); // Short timeout for test
        controller = new AsyncController();
        try {
            ServerIndexHandler.register(NanoHTTPD.Method.GET, "/async/simple", controller,
                    AsyncController.class.getMethod("simpleAsync"));
            ServerIndexHandler.register(NanoHTTPD.Method.GET, "/async/entity", controller,
                    AsyncController.class.getMethod("entityAsync"));
            ServerIndexHandler.register(NanoHTTPD.Method.GET, "/async/timeout", controller,
                    AsyncController.class.getMethod("timeoutAsync"));
            ServerIndexHandler.register(NanoHTTPD.Method.GET, "/async/error", controller,
                    AsyncController.class.getMethod("errorAsync"));
        } catch (NoSuchMethodException e) {
            fail("Failed to setup test methods: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        Environment.reset();
    }

    @Test
    public void testSimpleAsyncResponse() throws IOException {
        NanoHTTPD.IHTTPSession session = createMockSession("/async/simple");
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());

        String body = readBody(response);
        assertEquals("\"Async Hello World\"", body);
    }

    @Test
    public void testResponseEntityAsync() throws IOException {
        NanoHTTPD.IHTTPSession session = createMockSession("/async/entity");
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(201, response.getStatus().getRequestStatus());

        String body = readBody(response);
        assertTrue(body.contains("\"printed\":true"));
        assertTrue(body.contains("\"jobId\":\"POS-9988\""));
    }

    @Test
    public void testAsyncTimeoutReturns408() throws IOException {
        NanoHTTPD.IHTTPSession session = createMockSession("/async/timeout");
        long start = System.currentTimeMillis();
        NanoHTTPD.Response response = ServerIndexHandler.process(session);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertEquals(408, response.getStatus().getRequestStatus());
        assertTrue("Should have waited around 1 second before timeout", elapsed >= 900);

        String body = readBody(response);
        assertTrue(body.contains("Request Timeout"));
    }

    @Test
    public void testAsyncExceptionReturns500() throws IOException {
        NanoHTTPD.IHTTPSession session = createMockSession("/async/error");
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.INTERNAL_ERROR, response.getStatus());

        String body = readBody(response);
        assertTrue(body.contains("POS printer out of paper"));
    }

    private String readBody(NanoHTTPD.Response response) throws IOException {
        InputStream is = response.getData();
        if (is == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toString("UTF-8");
    }

    private NanoHTTPD.IHTTPSession createMockSession(String uri) {
        return (NanoHTTPD.IHTTPSession) java.lang.reflect.Proxy.newProxyInstance(
                NanoHTTPD.IHTTPSession.class.getClassLoader(),
                new Class<?>[]{NanoHTTPD.IHTTPSession.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getUri")) return uri;
                    if (name.equals("getMethod")) return NanoHTTPD.Method.GET;
                    if (name.equals("getHeaders")) return Collections.emptyMap();
                    if (name.equals("getParms")) return Collections.emptyMap();
                    if (name.equals("getRemoteIpAddress")) return "127.0.0.1";
                    if (name.equals("getRemoteHostName")) return "localhost";
                    return null;
                }
        );
    }
}
