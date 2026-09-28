package com.github.matheuscruzsouza.nanospring.http;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResponseEntityTest {

    @Test
    public void testOkResponseEntity() {
        ResponseEntity<String> response = ResponseEntity.ok("Hello World");
        assertEquals(200, response.getStatusCode());
        assertEquals("Hello World", response.getBody());
    }

    @Test
    public void testCreatedResponseEntity() {
        ResponseEntity<Integer> response = ResponseEntity.created(42);
        assertEquals(201, response.getStatusCode());
        assertEquals(Integer.valueOf(42), response.getBody());
    }

    @Test
    public void testCustomStatusCodeAndHeaders() {
        ResponseEntity<String> response = ResponseEntity.<String>status(418)
                .header("X-Custom-Header", "CustomValue")
                .body("I'm a teapot");

        assertEquals(418, response.getStatusCode());
        assertEquals("I'm a teapot", response.getBody());
        assertEquals("CustomValue", response.getHeaders().get("X-Custom-Header"));
    }

    @Test
    public void testNotFoundBuild() {
        ResponseEntity<Void> response = ResponseEntity.notFound().build();
        assertEquals(404, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testHttpStatusValueOf() {
        assertEquals(HttpStatus.OK, HttpStatus.valueOf(200));
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(404));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.valueOf(500));
        assertNull(HttpStatus.valueOf(999));
    }

    @Test
    public void testCustomHttpStatus() {
        var status = HttpStatus.custom(299, "Special OK");
        assertEquals(299, status.getRequestStatus());
        assertEquals("299 Special OK", status.getDescription());
    }
}
