package com.github.matheuscruzsouza.nanospring.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ResponseEntity<T> {

    private final NanoHTTPD.Response.IStatus status;
    private final Map<String, String> headers;
    private final T body;

    public ResponseEntity(T body, NanoHTTPD.Response.IStatus status) {
        this(body, null, status);
    }

    public ResponseEntity(T body, Map<String, String> headers, NanoHTTPD.Response.IStatus status) {
        this.body = body;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.status = status != null ? status : HttpStatus.OK;
    }

    public ResponseEntity(T body, int statusCode) {
        this(body, null, resolveStatus(statusCode));
    }

    public NanoHTTPD.Response.IStatus getStatus() {
        return status;
    }

    public int getStatusCode() {
        return status.getRequestStatus();
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public T getBody() {
        return body;
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    public static <T> ResponseEntity<T> ok() {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    public static <T> BodyBuilder status(HttpStatus status) {
        return new DefaultBodyBuilder(status);
    }

    public static <T> BodyBuilder status(int statusCode) {
        return new DefaultBodyBuilder(resolveStatus(statusCode));
    }

    public static <T> BodyBuilder status(NanoHTTPD.Response.IStatus status) {
        return new DefaultBodyBuilder(status);
    }

    public static <T> BodyBuilder notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    public static <T> BodyBuilder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<T> noContent() {
        return status(HttpStatus.NO_CONTENT).build();
    }

    private static NanoHTTPD.Response.IStatus resolveStatus(int statusCode) {
        HttpStatus known = HttpStatus.valueOf(statusCode);
        if (known != null) return known;
        return HttpStatus.custom(statusCode, "Custom Status");
    }

    public interface BodyBuilder {
        BodyBuilder header(String headerName, String headerValue);
        <T> ResponseEntity<T> body(T body);
        <T> ResponseEntity<T> build();
    }

    private static class DefaultBodyBuilder implements BodyBuilder {
        private final NanoHTTPD.Response.IStatus status;
        private final Map<String, String> headers = new HashMap<>();

        public DefaultBodyBuilder(NanoHTTPD.Response.IStatus status) {
            this.status = status;
        }

        @Override
        public BodyBuilder header(String headerName, String headerValue) {
            this.headers.put(headerName, headerValue);
            return this;
        }

        @Override
        public <T> ResponseEntity<T> body(T body) {
            return new ResponseEntity<>(body, this.headers, this.status);
        }

        @Override
        public <T> ResponseEntity<T> build() {
            return new ResponseEntity<>(null, this.headers, this.status);
        }
    }
}
