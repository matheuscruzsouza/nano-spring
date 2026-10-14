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
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.github.matheuscruzsouza.nanospring.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.validation.Valid;

import static org.junit.Assert.*;

public class FormUrlEncodedRequestBodyTest {

    public static class TestIssueDto {
        @NotNull(message = "projectKey cannot be null")
        private String projectKey;
        private String title;

        public String getProjectKey() {
            return projectKey;
        }

        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class FormTestController {

        @PostMethod("/test/form-string")
        public ResponseEntity<String> receiveRawString(@RequestBody String body) {
            return ResponseEntity.ok(body);
        }

        @PostMethod("/test/form-map")
        public ResponseEntity<String> receiveMap(@RequestBody Map<String, String> body) {
            return ResponseEntity.ok(body.get("projectKey") + ":" + body.get("title"));
        }

        @PostMethod("/test/form-dto")
        public ResponseEntity<String> receiveDto(@Valid @RequestBody TestIssueDto dto) {
            return ResponseEntity.ok("CREATED:" + dto.getProjectKey() + ":" + dto.getTitle());
        }

        @PostMethod("/test/request-param-map")
        public ResponseEntity<String> receiveRequestParamMap(@RequestParam Map<String, String> params) {
            return ResponseEntity.ok("PARAM:" + params.get("projectKey"));
        }

        @PostMethod("/test/json-body")
        public ResponseEntity<String> receiveJsonDto(@RequestBody TestIssueDto dto) {
            return ResponseEntity.ok("JSON:" + dto.getProjectKey() + ":" + dto.getTitle());
        }
    }

    private FormTestController controller;

    @Before
    public void setup() throws Exception {
        controller = new FormTestController();

        Method formStringMethod = FormTestController.class.getMethod("receiveRawString", String.class);
        Method formMapMethod = FormTestController.class.getMethod("receiveMap", Map.class);
        Method formDtoMethod = FormTestController.class.getMethod("receiveDto", TestIssueDto.class);
        Method paramMapMethod = FormTestController.class.getMethod("receiveRequestParamMap", Map.class);
        Method jsonBodyMethod = FormTestController.class.getMethod("receiveJsonDto", TestIssueDto.class);

        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/test/form-string", controller, formStringMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/test/form-map", controller, formMapMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/test/form-dto", controller, formDtoMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/test/request-param-map", controller, paramMapMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/test/json-body", controller, jsonBodyMethod);
    }

    private NanoHTTPD.IHTTPSession createMockSession(String uri, Map<String, String> parms, Map<String, String> files) {
        return (NanoHTTPD.IHTTPSession) Proxy.newProxyInstance(
                FormUrlEncodedRequestBodyTest.class.getClassLoader(),
                new Class<?>[]{NanoHTTPD.IHTTPSession.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()) {
                            case "getUri":
                                return uri;
                            case "getMethod":
                                return NanoHTTPD.Method.POST;
                            case "getParms":
                                return parms != null ? parms : Collections.emptyMap();
                            case "getHeaders":
                                return Collections.emptyMap();
                            case "parseBody":
                                if (files != null && args != null && args.length > 0 && args[0] instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> target = (Map<String, String>) args[0];
                                    target.putAll(files);
                                }
                                return null;
                            default:
                                return null;
                        }
                    }
                }
        );
    }

    @Test
    public void testRequestBodyReceivesFormUrlEncodedAsString() {
        Map<String, String> formParms = new HashMap<>();
        formParms.put("projectKey", "MOB");
        formParms.put("title", "Novo Ticket");

        // files is empty for application/x-www-form-urlencoded
        Map<String, String> files = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/form-string", formParms, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testRequestBodyReceivesFormUrlEncodedAsMap() {
        Map<String, String> formParms = new HashMap<>();
        formParms.put("projectKey", "MOB");
        formParms.put("title", "Novo Ticket");

        Map<String, String> files = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/form-map", formParms, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testRequestBodyReceivesFormUrlEncodedAsDtoWithValidation() {
        Map<String, String> formParms = new HashMap<>();
        formParms.put("projectKey", "MOB");
        formParms.put("title", "Novo Ticket");

        Map<String, String> files = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/form-dto", formParms, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testRequestBodyFormUrlEncodedDtoValidationError() {
        Map<String, String> formParms = new HashMap<>();
        // projectKey missing -> triggers @NotNull validation failure
        formParms.put("title", "Novo Ticket");

        Map<String, String> files = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/form-dto", formParms, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(400, response.getStatus().getRequestStatus());
    }

    @Test
    public void testRequestParamMapInjection() {
        Map<String, String> formParms = new HashMap<>();
        formParms.put("projectKey", "MOB-PARAMS");

        Map<String, String> files = new HashMap<>();

        NanoHTTPD.IHTTPSession session = createMockSession("/test/request-param-map", formParms, files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }

    @Test
    public void testJsonRequestBodyContinuesWorking() {
        Map<String, String> files = new HashMap<>();
        files.put("postData", "{\"projectKey\":\"JSON-KEY\",\"title\":\"JSON Title\"}");

        NanoHTTPD.IHTTPSession session = createMockSession("/test/json-body", Collections.emptyMap(), files);
        NanoHTTPD.Response response = ServerIndexHandler.process(session);

        assertNotNull(response);
        assertEquals(200, response.getStatus().getRequestStatus());
    }
}
