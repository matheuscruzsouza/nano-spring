package com.github.matheuscruzsouza.nanospring.security;

import android.content.Context;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;

/**
 * Handles SSL/TLS configuration for Nano-Spring.
 * Loads KeyStore from assets, local storage, or classpath, and creates SSLServerSocketFactory.
 */
public class SslConfiguration {

    private static final String TAG = "SslConfiguration";
    public static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";

    private final boolean enabled;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String keyStoreType;
    private final String protocols;

    public SslConfiguration() {
        this.enabled = Boolean.parseBoolean(Environment.getProperty("server.ssl.enabled", "false"));
        this.keyStorePath = Environment.getProperty("server.ssl.key-store", null);
        this.keyStorePassword = Environment.getProperty("server.ssl.key-store-password", "");
        this.keyStoreType = Environment.getProperty("server.ssl.key-store-type", DEFAULT_KEYSTORE_TYPE);
        this.protocols = Environment.getProperty("server.ssl.protocols", null);
    }

    public SslConfiguration(boolean enabled, String keyStorePath, String keyStorePassword, String keyStoreType, String protocols) {
        this.enabled = enabled;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyStoreType = keyStoreType != null ? keyStoreType : DEFAULT_KEYSTORE_TYPE;
        this.protocols = protocols;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String[] getProtocols() {
        if (protocols != null && !protocols.trim().isEmpty()) {
            return protocols.split(",");
        }
        return null;
    }

    public SSLServerSocketFactory createSslSocketFactory(Context context) {
        if (!enabled) return null;

        if (keyStorePath == null || keyStorePath.trim().isEmpty()) {
            Log.e(TAG, "SSL is enabled but server.ssl.key-store is not specified!");
            return null;
        }

        InputStream is = null;
        try {
            // 1. Try Android Assets
            if (context != null) {
                try {
                    is = context.getAssets().open(keyStorePath);
                } catch (Exception ignored) {
                }
            }

            // 2. Try File System Path
            if (is == null) {
                File f = new File(keyStorePath);
                if (f.exists() && f.isFile()) {
                    is = new FileInputStream(f);
                }
            }

            // 3. Try ClassLoader
            if (is == null) {
                is = SslConfiguration.class.getClassLoader().getResourceAsStream(keyStorePath);
            }

            if (is == null) {
                Log.e(TAG, "Could not locate keystore file at: " + keyStorePath);
                return null;
            }

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            char[] passwordChars = keyStorePassword != null ? keyStorePassword.toCharArray() : new char[0];
            keyStore.load(is, passwordChars);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, passwordChars);

            return NanoHTTPD.makeSSLSocketFactory(keyStore, kmf);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SSL Socket Factory: " + e.getMessage(), e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
