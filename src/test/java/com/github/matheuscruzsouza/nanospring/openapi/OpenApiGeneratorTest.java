package com.github.matheuscruzsouza.nanospring.openapi;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestHeader;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.ResponseStatus;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.handler.ServerIndexHandler;
import com.github.matheuscruzsouza.nanospring.http.HttpStatus;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.ApiResponse;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Parameter;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag;
import com.github.matheuscruzsouza.nanospring.server.Environment;
import com.github.matheuscruzsouza.nanospring.validation.Email;
import com.github.matheuscruzsouza.nanospring.validation.Max;
import com.github.matheuscruzsouza.nanospring.validation.Min;
import com.github.matheuscruzsouza.nanospring.validation.NotBlank;
import com.github.matheuscruzsouza.nanospring.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.validation.Size;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.*;

public class OpenApiGeneratorTest {

    public static class TestUserDto {
        @NotBlank
        @Size(min = 2, max = 50)
        private String name;

        @NotNull
        @Email
        private String email;

        @Min(18)
        @Max(120)
        private int age;

        @Parameter(description = "User role", example = "ADMIN")
        private String role;
    }

    @Tag(name = "Users", description = "User management operations")
    @RestController("/api/users")
    public static class TestUserController {

        @Operation(summary = "Get user by ID", description = "Returns single user detail")
        @GetMethod("/:id")
        public ResponseEntity<TestUserDto> getUser(
                @PathVariable("id") @Parameter(description = "Numeric user ID", example = "42") Long id,
                @RequestParam("active") Boolean active,
                @RequestHeader(value = "X-Api-Key", required = false) String apiKey
        ) {
            return ResponseEntity.ok(new TestUserDto());
        }

        @Operation(summary = "Create user", description = "Creates a new user account")
        @ResponseStatus(HttpStatus.CREATED)
        @ApiResponse(responseCode = 201, description = "User successfully created", responseClass = TestUserDto.class)
        @ApiResponse(responseCode = 400, description = "Validation failed")
        @PostMethod("")
        public TestUserDto createUser(@RequestBody TestUserDto body) {
            return body;
        }
    }

    @Before
    public void setUp() throws Exception {
        ServerIndexHandler.clearInstances();
        Environment.setProperty("nano.swagger.title", "Test Store API");
        Environment.setProperty("nano.swagger.version", "2.5.0");
        Environment.setProperty("nano.swagger.description", "Unit test API specification");

        TestUserController controller = new TestUserController();
        Method getUserMethod = TestUserController.class.getMethod("getUser", Long.class, Boolean.class, String.class);
        Method createUserMethod = TestUserController.class.getMethod("createUser", TestUserDto.class);

        ServerIndexHandler.register(NanoHTTPD.Method.GET, "/api/users/:id", controller, getUserMethod);
        ServerIndexHandler.register(NanoHTTPD.Method.POST, "/api/users", controller, createUserMethod);
    }

    @After
    public void tearDown() {
        ServerIndexHandler.clearInstances();
    }

    @Test
    public void testOpenApiSpecMetadata() {
        Map<String, Object> spec = OpenApiGenerator.generateSpec();
        assertEquals("3.0.1", spec.get("openapi"));

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) spec.get("info");
        assertNotNull(info);
        assertEquals("Test Store API", info.get("title"));
        assertEquals("2.5.0", info.get("version"));
        assertEquals("Unit test API specification", info.get("description"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> servers = (List<Map<String, Object>>) spec.get("servers");
        assertNotNull(servers);
        assertEquals(1, servers.size());
        assertEquals("/", servers.get(0).get("url"));
    }

    @Test
    public void testPathAndOperationMapping() {
        Map<String, Object> spec = OpenApiGenerator.generateSpec();

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        assertNotNull(paths);
        assertTrue(paths.containsKey("/api/users/{id}"));
        assertTrue(paths.containsKey("/api/users"));

        @SuppressWarnings("unchecked")
        Map<String, Object> getUserPath = (Map<String, Object>) paths.get("/api/users/{id}");
        @SuppressWarnings("unchecked")
        Map<String, Object> getOp = (Map<String, Object>) getUserPath.get("get");
        assertNotNull(getOp);
        assertEquals("Get user by ID", getOp.get("summary"));
        assertEquals("Returns single user detail", getOp.get("description"));

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) getOp.get("tags");
        assertEquals("Users", tags.get(0));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) getOp.get("parameters");
        assertNotNull(params);
        assertEquals(3, params.size());

        // Path param
        Map<String, Object> pId = params.get(0);
        assertEquals("id", pId.get("name"));
        assertEquals("path", pId.get("in"));
        assertTrue((Boolean) pId.get("required"));
        assertEquals("Numeric user ID", pId.get("description"));
        assertEquals("42", pId.get("example"));

        // Query param
        Map<String, Object> pActive = params.get(1);
        assertEquals("active", pActive.get("name"));
        assertEquals("query", pActive.get("in"));
        assertFalse((Boolean) pActive.get("required"));

        // Header param
        Map<String, Object> pHeader = params.get(2);
        assertEquals("X-Api-Key", pHeader.get("name"));
        assertEquals("header", pHeader.get("in"));
        assertFalse((Boolean) pHeader.get("required"));
    }

    @Test
    public void testRequestBodyAndSchemaGeneration() {
        Map<String, Object> spec = OpenApiGenerator.generateSpec();

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        @SuppressWarnings("unchecked")
        Map<String, Object> postOp = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/users")).get("post");
        assertNotNull(postOp);

        @SuppressWarnings("unchecked")
        Map<String, Object> reqBody = (Map<String, Object>) postOp.get("requestBody");
        assertNotNull(reqBody);
        assertTrue((Boolean) reqBody.get("required"));

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        assertNotNull(components);
        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertNotNull(schemas);
        assertTrue(schemas.containsKey("TestUserDto"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userDtoSchema = (Map<String, Object>) schemas.get("TestUserDto");
        assertEquals("object", userDtoSchema.get("type"));

        @SuppressWarnings("unchecked")
        List<String> requiredFields = (List<String>) userDtoSchema.get("required");
        assertNotNull(requiredFields);
        assertTrue(requiredFields.contains("name"));
        assertTrue(requiredFields.contains("email"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) userDtoSchema.get("properties");
        assertNotNull(props);

        @SuppressWarnings("unchecked")
        Map<String, Object> nameProp = (Map<String, Object>) props.get("name");
        assertEquals("string", nameProp.get("type"));
        assertEquals(2, ((Number) nameProp.get("minLength")).intValue());
        assertEquals(50, ((Number) nameProp.get("maxLength")).intValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> emailProp = (Map<String, Object>) props.get("email");
        assertEquals("email", emailProp.get("format"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ageProp = (Map<String, Object>) props.get("age");
        assertEquals(18L, ((Number) ageProp.get("minimum")).longValue());
        assertEquals(120L, ((Number) ageProp.get("maximum")).longValue());
    }

    @Test
    public void testCustomApiResponseAnnotations() {
        Map<String, Object> spec = OpenApiGenerator.generateSpec();

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        @SuppressWarnings("unchecked")
        Map<String, Object> postOp = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/users")).get("post");
        assertNotNull(postOp);

        @SuppressWarnings("unchecked")
        Map<String, Object> responses = (Map<String, Object>) postOp.get("responses");
        assertNotNull(responses);
        assertTrue(responses.containsKey("201"));
        assertTrue(responses.containsKey("400"));

        @SuppressWarnings("unchecked")
        Map<String, Object> r201 = (Map<String, Object>) responses.get("201");
        assertEquals("User successfully created", r201.get("description"));
    }

    @Test
    public void testJsonSerialization() {
        String json = OpenApiGenerator.generateSpecJson(true);
        assertNotNull(json);
        assertTrue(json.contains("\"openapi\": \"3.0.1\""));
        assertTrue(json.contains("\"Test Store API\""));

        // Must be parseable valid JSON
        Map<?, ?> parsed = new Gson().fromJson(json, Map.class);
        assertEquals("3.0.1", parsed.get("openapi"));
    }
}
