package com.github.matheuscruzsouza.nanospring.http;

import fi.iki.elonen.NanoHTTPD;

public enum HttpStatus implements NanoHTTPD.Response.IStatus {
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    NOT_MODIFIED(304, "Not Modified"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONFLICT(409, "Conflict"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    private final int code;
    private final String description;

    HttpStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getRequestStatus() {
        return code;
    }

    @Override
    public String getDescription() {
        return code + " " + description;
    }

    public int value() {
        return code;
    }

    public static HttpStatus valueOf(int statusCode) {
        for (HttpStatus status : values()) {
            if (status.code == statusCode) {
                return status;
            }
        }
        return null;
    }

    public static NanoHTTPD.Response.IStatus custom(int code, String description) {
        return new NanoHTTPD.Response.IStatus() {
            @Override
            public int getRequestStatus() {
                return code;
            }

            @Override
            public String getDescription() {
                return code + (description != null && !description.isEmpty() ? " " + description : "");
            }
        };
    }
}
