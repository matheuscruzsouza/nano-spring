package com.github.matheuscruzsouza.nanospring.handler;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import com.github.matheuscruzsouza.nanospring.annotation.ConnectMethod;
import com.github.matheuscruzsouza.nanospring.annotation.HeadMethod;
import com.github.matheuscruzsouza.nanospring.annotation.OptionsMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PatchMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.TraceMethod;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;

import static org.junit.Assert.*;

public class ExtendedHttpMethodsTest {

    public static class UpdateDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TestExtendedController {

        @PatchMethod("/test/patch")
        public ResponseEntity<String> handlePatch(@RequestBody UpdateDto dto) {
            return ResponseEntity.ok("PATCHED:" + dto.getName());
        }

        @HeadMethod("/test/head")
        public ResponseEntity<String> handleHead() {
            return ResponseEntity.ok("BODY_CONTENT_SHOULD_BE_CLEARED");
        }

        @OptionsMethod("/test/options")
        public ResponseEntity<String> handleOptions() {
            return ResponseEntity.ok("OPTIONS_ALLOWED");
        }

        @TraceMethod("/test/trace")
        public ResponseEntity<String> handleTrace() {
            return ResponseEntity.ok("TRACED");
        }

        @ConnectMethod("/test/connect")
        public ResponseEntity<String> handleConnect() {
            return ResponseEntity.ok("CONNECTED");
        }
    }

    private TestExtendedController controller;

    @Before
    public void setup() throws Exception {
        controller = new TestExtendedController();
        ServerIndexHandler.clearInstances();

        Method patchMethod = TestExtendedController.class.getMethod("handlePatch", UpdateDto.class);
        ServerIndexHandler.register(NanoHTTPD.Method.PATCH, "/test/patch", controller, patchMethod);

        Method headMethod = TestExtendedController.class.getMethod("handleHead");
        ServerIndexHandler.register(NanoHTTPD.Method.HEAD, "/test/head", controller, headMethod);

        Method optionsMethod = TestExtendedController.class.getMethod("handleOptions");
        ServerIndexHandler.register(NanoHTTPD.Method.OPTIONS, "/test/options", controller, optionsMethod);

        Method traceMethod = TestExtendedController.class.getMethod("handleTrace");
        ServerIndexHandler.register(NanoHTTPD.Method.TRACE, "/test/trace", controller, traceMethod);

        Method connectMethod = TestExtendedController.class.getMethod("handleConnect");
        ServerIndexHandler.register(NanoHTTPD.Method.CONNECT, "/test/connect", controller, connectMethod);
    }

    private NanoHTTPD.IHTTPSession createMockSession(
            NanoHTTPD.Method method,
            String uri,
            Map<String, String> headers,
            Map<String, String> bodyFiles) {

        return (NanoHTTPD.IHTTPSession) Proxy.newProxyInstance(
                NanoHTTPD.IHTTPSession.class.getClassLoader(),
                new Class<?>[]{NanoHTTPD.IHTTPSession.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                        String name = m.getName();
                        switch (name) {
                            case "getMethod":
                                return method;
                            case "getUri":
                                return uri;
                            case "getHeaders":
                                return headers != null ? headers : Collections.emptyMap();
                            case "getParms":
                                return Collections.emptyMap();
                            case "getParameters":
                                return Collections.emptyMap();
                            case "parseBody":
                                if (args != null && args.length > 0 && args[0] instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> target = (Map<String, String>) args[0];
                                    if (bodyFiles != null) {
                                        target.putAll(bodyFiles);
                                    }
                                }
                                return null;
                            case "getRemoteIpAddress":
                                return "127.0.0.1";
                            default:
                                if (m.getReturnType().equals(boolean.class)) return false;
                                if (m.getReturnType().equals(int.class)) return 0;
                                return null;
                        }
                    }
                }
        );
    }

    @Test
    public void testPatchMethod() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        Map<String, String> files = new HashMap<>();
        files.put("postData", "{\"name\":\"Nano\"}");

        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.PATCH, "/test/patch", headers, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testHeadMethodHasEmptyBody() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.HEAD, "/test/head", Collections.emptyMap(), null);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
        // Verify stream has 0 available bytes
        assertEquals(0, response.getData().available());
    }

    @Test
    public void testOptionsMethod() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.OPTIONS, "/test/options", Collections.emptyMap(), null);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testTraceMethod() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.TRACE, "/test/trace", Collections.emptyMap(), null);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testConnectMethod() throws Exception {
        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.CONNECT, "/test/connect", Collections.emptyMap(), null);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testMethodNotAllowedIncludesAllowHeader() throws Exception {
        // Envia GET para uma rota que só tem PATCH registrado (/test/patch)
        NanoHTTPD.IHTTPSession session = createMockSession(NanoHTTPD.Method.GET, "/test/patch", Collections.emptyMap(), null);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, response.getStatus());
        String allowHeader = response.getHeader("Allow");
        assertNotNull(allowHeader);
        assertTrue(allowHeader.contains("PATCH"));
    }
}
