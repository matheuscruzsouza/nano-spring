package com.github.matheuscruzsouza.nanospring.handler;

import fi.iki.elonen.NanoHTTPD;

public interface HandlerInterceptor {
    /**
     * Called before the actual handler is executed.
     * @return true to continue processing, false to abort and optionally send a response directly.
     */
    boolean preHandle(NanoHTTPD.IHTTPSession session, String path);
}
