package com.github.matheuscruzsouza.nanospring.http;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import fi.iki.elonen.NanoHTTPD;

public class ResponseEntity<T> {

    private final NanoHTTPD.Response.IStatus status;
    private final Map<String, String> headers;
    private final T body;

    public ResponseEntity(NanoHTTPD.Response.IStatus status) {
        this(null, null, status);
    }

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

    public ResponseEntity(int statusCode) {
        this(null, null, resolveStatus(statusCode));
    }

    public NanoHTTPD.Response.IStatus getStatus() {
        return status;
    }

    public int getStatusCode() {
        return status.getRequestStatus();
    }

    public int getStatusCodeValue() {
        return status.getRequestStatus();
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public T getBody() {
        return body;
    }

    public boolean hasBody() {
        return body != null;
    }

    // Static Factory Methods

    public static BodyBuilder ok() {
        return status(HttpStatus.OK);
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    public static BodyBuilder created(URI location) {
        BodyBuilder builder = status(HttpStatus.CREATED);
        if (location != null) {
            builder.location(location);
        }
        return builder;
    }

    public static BodyBuilder created(String location) {
        BodyBuilder builder = status(HttpStatus.CREATED);
        if (location != null) {
            builder.location(location);
        }
        return builder;
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    public static BodyBuilder accepted() {
        return status(HttpStatus.ACCEPTED);
    }

    public static HeadersBuilder<?> noContent() {
        return status(HttpStatus.NO_CONTENT);
    }

    public static BodyBuilder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    public static HeadersBuilder<?> notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    public static BodyBuilder unprocessableEntity() {
        return status(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BodyBuilder internalServerError() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static BodyBuilder status(HttpStatus status) {
        return new DefaultBodyBuilder(status);
    }

    public static BodyBuilder status(int statusCode) {
        return new DefaultBodyBuilder(resolveStatus(statusCode));
    }

    public static BodyBuilder status(NanoHTTPD.Response.IStatus status) {
        return new DefaultBodyBuilder(status);
    }

    public static <T> ResponseEntity<T> of(Optional<T> optional) {
        if (optional != null && optional.isPresent()) {
            return ok(optional.get());
        }
        return notFound().build();
    }

    private static NanoHTTPD.Response.IStatus resolveStatus(int statusCode) {
        HttpStatus known = HttpStatus.valueOf(statusCode);
        if (known != null) return known;
        return HttpStatus.custom(statusCode, "Custom Status");
    }

    // Builder Interfaces

    public interface HeadersBuilder<B extends HeadersBuilder<B>> {
        B header(String headerName, String headerValue);
        B headers(Map<String, String> headers);
        B contentType(String contentType);
        B contentLength(long contentLength);
        B location(URI location);
        B location(String location);
        B cacheControl(String cacheControl);
        <T> ResponseEntity<T> build();
    }

    public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {
        <T> ResponseEntity<T> body(T body);
    }

    private static class DefaultBodyBuilder implements BodyBuilder {
        private final NanoHTTPD.Response.IStatus status;
        private final Map<String, String> headers = new HashMap<>();

        public DefaultBodyBuilder(NanoHTTPD.Response.IStatus status) {
            this.status = status;
        }

        @Override
        public BodyBuilder header(String headerName, String headerValue) {
            if (headerName != null && headerValue != null) {
                this.headers.put(headerName, headerValue);
            }
            return this;
        }

        @Override
        public BodyBuilder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        @Override
        public BodyBuilder contentType(String contentType) {
            return header("Content-Type", contentType);
        }

        @Override
        public BodyBuilder contentLength(long contentLength) {
            return header("Content-Length", String.valueOf(contentLength));
        }

        @Override
        public BodyBuilder location(URI location) {
            return header("Location", location != null ? location.toString() : "");
        }

        @Override
        public BodyBuilder location(String location) {
            return header("Location", location != null ? location : "");
        }

        @Override
        public BodyBuilder cacheControl(String cacheControl) {
            return header("Cache-Control", cacheControl);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseEntity)) return false;
        ResponseEntity<?> that = (ResponseEntity<?>) o;
        return Objects.equals(status, that.status) &&
               Objects.equals(headers, that.headers) &&
               Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, headers, body);
    }

    @Override
    public String toString() {
        return "<" + status + "," + (body != null ? body : "") + "," + headers + ">";
    }
}
