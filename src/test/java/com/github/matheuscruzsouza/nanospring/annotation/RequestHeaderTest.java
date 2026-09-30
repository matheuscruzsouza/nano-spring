package com.github.matheuscruzsouza.nanospring.annotation;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import com.github.matheuscruzsouza.nanospring.handler.ServerIndexHandler;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;

import static org.junit.Assert.*;

public class RequestHeaderTest {

    public static class TestHeaderController {

        @GetMethod("/test/auth")
        public ResponseEntity<String> testAuth(@RequestHeader("Authorization") String auth) {
            return ResponseEntity.ok("Token: " + auth);
        }

        @GetMethod("/test/port")
        public ResponseEntity<Integer> testPort(@RequestHeader(value = "X-Port", defaultValue = "8080") int port) {
            return ResponseEntity.ok(port);
        }

        @GetMethod("/test/optional")
        public ResponseEntity<String> testOptional(@RequestHeader(value = "X-Optional", required = false) String opt) {
            return ResponseEntity.ok(opt != null ? opt : "empty");
        }

        @GetMethod("/test/all-headers")
        public ResponseEntity<String> testAllHeaders(@RequestHeader Map<String, String> headers) {
            return ResponseEntity.ok("Count: " + headers.size());
        }
    }

    private TestHeaderController controller;

    @Before
    public void setup() throws Exception {
        controller = new TestHeaderController();
        Method authMethod = TestHeaderController.class.getMethod("testAuth", String.class);
        Method portMethod = TestHeaderController.class.getMethod("testPort", int.class);
        Method optMethod = TestHeaderController.class.getMethod("testOptional", String.class);
        Method allHeadersMethod = TestHeaderController.class.getMethod("testAllHeaders", Map.class);

        ServerIndexHandler.register(NanoHTTPD.Method.GET, "/test/auth", controller, authMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.GET, "/test/port", controller, portMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.GET, "/test/optional", controller, optMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.GET, "/test/all-headers", controller, allHeadersMethod);
    }

    private NanoHTTPD.IHTTPSession createMockSession(String uri, Map<String, String> headers) {
        return (NanoHTTPD.IHTTPSession) Proxy.newProxyInstance(
                NanoHTTPD.IHTTPSession.class.getClassLoader(),
                new Class<?>[]{NanoHTTPD.IHTTPSession.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if (name.equals("getUri")) return uri;
                        if (name.equals("getMethod")) return NanoHTTPD.Method.GET;
                        if (name.equals("getHeaders")) return headers != null ? headers : new HashMap<>();
                        if (name.equals("getParms")) return new HashMap<String, String>();
                        return null;
                    }
                }
        );
    }

    @Test
    public void testRequiredHeaderPresent() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer xyz123");

        NanoHTTPD.IHTTPSession session = createMockSession("/test/auth", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testCaseInsensitiveHeaderResolution() {
        Map<String, String> headers = new HashMap<>();
        headers.put("AUTHORIZATION", "Bearer CAPS");

        NanoHTTPD.IHTTPSession session = createMockSession("/test/auth", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testRequiredHeaderMissingReturns400() {
        Map<String, String> headers = new HashMap<>(); // No authorization header

        NanoHTTPD.IHTTPSession session = createMockSession("/test/auth", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDefaultValueWhenHeaderMissing() {
        Map<String, String> headers = new HashMap<>(); // No X-Port header

        NanoHTTPD.IHTTPSession session = createMockSession("/test/port", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testDefaultValueOverriddenWhenHeaderPresent() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-port", "9090");

        NanoHTTPD.IHTTPSession session = createMockSession("/test/port", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testOptionalHeaderMissing() {
        Map<String, String> headers = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/optional", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testMapAllHeadersInjection() {
        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0");
        headers.put("host", "localhost");

        NanoHTTPD.IHTTPSession session = createMockSession("/test/all-headers", headers);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }
}
