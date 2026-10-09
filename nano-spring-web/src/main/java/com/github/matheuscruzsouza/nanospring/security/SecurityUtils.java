package com.github.matheuscruzsouza.nanospring.security;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SecurityUtils {

    /**
     * Verifies Basic Auth using `nano.security.username` and `nano.security.password`.
     * If these properties are not set, defaults to "admin" / "admin".
     */
    public static boolean checkBasicAuth(NanoHTTPD.IHTTPSession session) {
        String expectedUser = Environment.getProperty("nano.security.username", "admin");
        String expectedPass = Environment.getProperty("nano.security.password", "admin");
        
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");
        if (authHeader == null) {
            authHeader = headers.get("Authorization");
        }
        
        if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] credDecoded;
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    credDecoded = java.util.Base64.getDecoder().decode(base64Credentials);
                } else {
                    credDecoded = android.util.Base64.decode(base64Credentials, android.util.Base64.DEFAULT);
                }
                String credentials = new String(credDecoded, "UTF-8");
                final String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    return expectedUser.equals(username) && expectedPass.equals(password);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
