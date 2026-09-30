package com.github.matheuscruzsouza.nanospring.http;

import org.junit.Test;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class ResponseEntityTest {

    @Test
    public void testOkResponseEntity() {
        ResponseEntity<String> response = ResponseEntity.ok("Hello World");
        assertEquals(200, response.getStatusCode());
        assertEquals("Hello World", response.getBody());
        assertTrue(response.hasBody());
    }

    @Test
    public void testOkBuilder() {
        ResponseEntity<String> response = ResponseEntity.ok()
                .header("X-Custom", "Val")
                .body("Built OK");
        assertEquals(200, response.getStatusCode());
        assertEquals("Built OK", response.getBody());
        assertEquals("Val", response.getHeaders().get("X-Custom"));

        ResponseEntity<Void> emptyOk = ResponseEntity.ok().build();
        assertEquals(200, emptyOk.getStatusCode());
        assertNull(emptyOk.getBody());
        assertFalse(emptyOk.hasBody());
    }

    @Test
    public void testCreatedResponseEntity() {
        ResponseEntity<Integer> response = ResponseEntity.created(42);
        assertEquals(201, response.getStatusCode());
        assertEquals(Integer.valueOf(42), response.getBody());
    }

    @Test
    public void testCreatedWithLocation() {
        URI uri = URI.create("/api/items/100");
        ResponseEntity<String> response = ResponseEntity.created(uri).body("Novo Item");
        assertEquals(201, response.getStatusCode());
        assertEquals("/api/items/100", response.getHeaders().get("Location"));
        assertEquals("Novo Item", response.getBody());

        ResponseEntity<Void> fromString = ResponseEntity.created("/api/items/200").build();
        assertEquals(201, fromString.getStatusCode());
        assertEquals("/api/items/200", fromString.getHeaders().get("Location"));
        assertNull(fromString.getBody());
    }

    @Test
    public void testAccepted() {
        ResponseEntity<String> response = ResponseEntity.accepted().body("Processando");
        assertEquals(202, response.getStatusCode());
        assertEquals("Processando", response.getBody());
    }

    @Test
    public void testNoContent() {
        ResponseEntity<Void> response = ResponseEntity.noContent().build();
        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testBadRequest() {
        ResponseEntity<Map<String, String>> response = ResponseEntity.badRequest()
                .body(Map.of("error", "Bad input"));
        assertEquals(400, response.getStatusCode());
        assertEquals("Bad input", response.getBody().get("error"));
    }

    @Test
    public void testNotFoundBuild() {
        ResponseEntity<Void> response = ResponseEntity.notFound().build();
        assertEquals(404, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testUnprocessableEntity() {
        ResponseEntity<String> response = ResponseEntity.unprocessableEntity().body("Invalido");
        assertEquals(422, response.getStatusCode());
        assertEquals("Invalido", response.getBody());
    }

    @Test
    public void testInternalServerError() {
        ResponseEntity<String> response = ResponseEntity.internalServerError().body("Erro");
        assertEquals(500, response.getStatusCode());
        assertEquals("Erro", response.getBody());
    }

    @Test
    public void testCustomStatusCodeAndHeaders() {
        ResponseEntity<String> response = ResponseEntity.status(418)
                .header("X-Custom-Header", "CustomValue")
                .body("I'm a teapot");

        assertEquals(418, response.getStatusCode());
        assertEquals(418, response.getStatusCodeValue());
        assertEquals("I'm a teapot", response.getBody());
        assertEquals("CustomValue", response.getHeaders().get("X-Custom-Header"));
    }

    @Test
    public void testHeadersConvenienceMethods() {
        ResponseEntity<String> response = ResponseEntity.ok()
                .contentType("text/csv")
                .contentLength(1024)
                .cacheControl("no-cache")
                .headers(Map.of("X-App", "NanoSpring", "X-Key", "Secret"))
                .body("id,nome\n1,Ana");

        assertEquals("text/csv", response.getHeaders().get("Content-Type"));
        assertEquals("1024", response.getHeaders().get("Content-Length"));
        assertEquals("no-cache", response.getHeaders().get("Cache-Control"));
        assertEquals("NanoSpring", response.getHeaders().get("X-App"));
        assertEquals("Secret", response.getHeaders().get("X-Key"));
    }

    @Test
    public void testOfOptional() {
        Optional<String> presente = Optional.of("Encontrado");
        ResponseEntity<String> res1 = ResponseEntity.of(presente);
        assertEquals(200, res1.getStatusCode());
        assertEquals("Encontrado", res1.getBody());

        Optional<String> vazio = Optional.empty();
        ResponseEntity<String> res2 = ResponseEntity.of(vazio);
        assertEquals(404, res2.getStatusCode());
        assertNull(res2.getBody());
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
