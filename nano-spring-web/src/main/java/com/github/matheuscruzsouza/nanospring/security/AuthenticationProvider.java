package com.github.matheuscruzsouza.nanospring.security;

import fi.iki.elonen.NanoHTTPD;

/**
 * Strategy interface for authenticating HTTP requests in Nano-Spring.
 * Implementations can validate HTTP Basic Auth, Bearer JWT tokens, API keys, etc.
 */
public interface AuthenticationProvider {

    /**
     * Authenticates an incoming HTTP request.
     *
     * @param session The HTTP session containing headers, URI, method, and parameters.
     * @return true if the request is successfully authenticated, false otherwise.
     */
    boolean authenticate(NanoHTTPD.IHTTPSession session);
}
