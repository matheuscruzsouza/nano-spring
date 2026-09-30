package com.github.matheuscruzsouza.nanospring.cors;

import java.util.Properties;

public final class CorsConfiguration {
    private final boolean enabled;
    private final String allowedOrigins;
    private final String allowedMethods;
    private final String allowedHeaders;
    private final String maxAge;

    public static final String KEY_ENABLED = "nano.cors.enabled";
    public static final String KEY_ORIGINS = "nano.cors.allowed-origins";
    public static final String KEY_METHODS = "nano.cors.allowed-methods";
    public static final String KEY_HEADERS = "nano.cors.allowed-headers";
    public static final String KEY_MAX_AGE = "nano.cors.max-age";

    public static final String DEFAULT_ORIGINS = "*";
    public static final String DEFAULT_METHODS = "GET,POST,PUT,DELETE,OPTIONS";
    public static final String DEFAULT_HEADERS = "Content-Type,Authorization,X-Requested-With,Accept";
    public static final String DEFAULT_MAX_AGE = "86400";

    public CorsConfiguration() {
        this(null);
    }

    public CorsConfiguration(Properties properties) {
        if (properties == null) {
            this.enabled = true;
            this.allowedOrigins = DEFAULT_ORIGINS;
            this.allowedMethods = DEFAULT_METHODS;
            this.allowedHeaders = DEFAULT_HEADERS;
            this.maxAge = DEFAULT_MAX_AGE;
            return;
        }
        this.enabled = Boolean.parseBoolean(properties.getProperty(KEY_ENABLED, "true"));
        this.allowedOrigins = properties.getProperty(KEY_ORIGINS, DEFAULT_ORIGINS);
        this.allowedMethods = properties.getProperty(KEY_METHODS, DEFAULT_METHODS);
        this.allowedHeaders = properties.getProperty(KEY_HEADERS, DEFAULT_HEADERS);
        this.maxAge = properties.getProperty(KEY_MAX_AGE, DEFAULT_MAX_AGE);
    }

    public boolean isEnabled() { return enabled; }
    public String getAllowedOrigins() { return allowedOrigins; }
    public String getAllowedMethods() { return allowedMethods; }
    public String getAllowedHeaders() { return allowedHeaders; }
    public String getMaxAge() { return maxAge; }
}
