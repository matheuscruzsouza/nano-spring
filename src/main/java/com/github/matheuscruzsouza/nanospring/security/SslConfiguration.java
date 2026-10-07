package com.github.matheuscruzsouza.nanospring.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.security.auth.x500.X500Principal;

import fi.iki.elonen.NanoHTTPD;

/**
 * Handles SSL/TLS configuration for Nano-Spring.
 * Loads KeyStore from assets, local storage, or classpath, and creates SSLServerSocketFactory.
 * Also supports AndroidKeyStore with auto-generation and external certificate import.
 */
public class SslConfiguration {

    private static final String TAG = "SslConfiguration";
    public static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    public static final String DEFAULT_KEY_ALIAS = "nanospring-ssl-key";

    private final boolean enabled;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String keyStoreType;
    private final String keyAlias;
    private final String protocols;

    public SslConfiguration() {
        this.enabled = Boolean.parseBoolean(Environment.getProperty("server.ssl.enabled", "false"));
        this.keyStorePath = Environment.getProperty("server.ssl.key-store", null);
        this.keyStorePassword = Environment.getProperty("server.ssl.key-store-password", "");
        this.keyStoreType = Environment.getProperty("server.ssl.key-store-type", DEFAULT_KEYSTORE_TYPE);
        this.keyAlias = Environment.getProperty("server.ssl.key-alias", DEFAULT_KEY_ALIAS);
        this.protocols = Environment.getProperty("server.ssl.protocols", null);
    }

    public SslConfiguration(boolean enabled, String keyStorePath, String keyStorePassword, String keyStoreType, String keyAlias, String protocols) {
        this.enabled = enabled;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyStoreType = keyStoreType != null ? keyStoreType : DEFAULT_KEYSTORE_TYPE;
        this.keyAlias = keyAlias != null ? keyAlias : DEFAULT_KEY_ALIAS;
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

    public String getKeyAlias() {
        return keyAlias;
    }

    public String[] getProtocols() {
        if (protocols != null && !protocols.trim().isEmpty()) {
            return protocols.split(",");
        }
        return null;
    }

    public SSLServerSocketFactory createSslSocketFactory(Context context) {
        if (!enabled) return null;

        try {
            KeyStore keyStore;

            if ("AndroidKeyStore".equalsIgnoreCase(keyStoreType)) {
                keyStore = handleAndroidKeyStore(context);
            } else {
                keyStore = loadExternalKeyStore(context);
            }

            if (keyStore == null) {
                return null;
            }

            char[] passwordChars = "AndroidKeyStore".equalsIgnoreCase(keyStoreType) ?
                    new char[0] : (keyStorePassword != null ? keyStorePassword.toCharArray() : new char[0]);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, passwordChars);

            return NanoHTTPD.makeSSLSocketFactory(keyStore, kmf);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SSL Socket Factory: " + e.getMessage(), e);
            return null;
        }
    }

    private KeyStore loadExternalKeyStore(Context context) throws Exception {
        if (keyStorePath == null || keyStorePath.trim().isEmpty()) {
            Log.e(TAG, "SSL is enabled but server.ssl.key-store is not specified!");
            return null;
        }

        InputStream is = null;
        try {
            if (context != null) {
                try {
                    is = context.getAssets().open(keyStorePath);
                } catch (Exception ignored) {
                }
            }

            if (is == null) {
                File f = new File(keyStorePath);
                if (f.exists() && f.isFile()) {
                    is = new FileInputStream(f);
                }
            }

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
            return keyStore;

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private KeyStore handleAndroidKeyStore(Context context) throws Exception {
        KeyStore androidKeyStore = KeyStore.getInstance("AndroidKeyStore");
        androidKeyStore.load(null);

        if (!androidKeyStore.containsAlias(keyAlias)) {
            Log.i(TAG, "Alias '" + keyAlias + "' not found in AndroidKeyStore.");

            if (keyStorePath != null && !keyStorePath.trim().isEmpty()) {
                Log.i(TAG, "Attempting to import external keystore into AndroidKeyStore...");
                importExternalKeyStore(context, androidKeyStore);
            } else {
                Log.i(TAG, "Generating self-signed certificate in AndroidKeyStore...");
                generateSelfSignedCertificate();
            }
        } else {
            Log.i(TAG, "Alias '" + keyAlias + "' found in AndroidKeyStore.");
        }

        return androidKeyStore;
    }

    private void importExternalKeyStore(Context context, KeyStore androidKeyStore) throws Exception {
        String originalType = keyStorePath.toLowerCase().endsWith(".bks") ? "BKS" : "PKCS12";
        KeyStore externalStore = KeyStore.getInstance(originalType);

        InputStream is = null;
        try {
            if (context != null) {
                try {
                    is = context.getAssets().open(keyStorePath);
                } catch (Exception ignored) {
                }
            }
            if (is == null) {
                File f = new File(keyStorePath);
                if (f.exists() && f.isFile()) {
                    is = new FileInputStream(f);
                }
            }
            if (is == null) {
                is = SslConfiguration.class.getClassLoader().getResourceAsStream(keyStorePath);
            }

            if (is == null) {
                throw new Exception("Could not locate external keystore file to import at: " + keyStorePath);
            }

            char[] passwordChars = keyStorePassword != null ? keyStorePassword.toCharArray() : new char[0];
            externalStore.load(is, passwordChars);

            String importAlias = null;
            java.util.Enumeration<String> aliases = externalStore.aliases();
            if (aliases.hasMoreElements()) {
                importAlias = aliases.nextElement();
            }

            if (importAlias == null) {
                throw new Exception("External keystore is empty, cannot import.");
            }

            PrivateKey privateKey = (PrivateKey) externalStore.getKey(importAlias, passwordChars);
            Certificate[] certificateChain = externalStore.getCertificateChain(importAlias);

            if (privateKey == null || certificateChain == null) {
                throw new Exception("Could not extract private key and certificate chain from external keystore.");
            }

            androidKeyStore.setKeyEntry(keyAlias, privateKey, null, certificateChain);
            Log.i(TAG, "Successfully imported external keystore into AndroidKeyStore with alias: " + keyAlias);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void generateSelfSignedCertificate() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.YEAR, 10);
        Date expiration = cal.getTime();

        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setCertificateSubject(new X500Principal("CN=localhost, O=Nano-Spring"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(now)
                .setCertificateNotAfter(expiration)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .build();

        kpg.initialize(spec);
        kpg.generateKeyPair();

        Log.i(TAG, "Successfully generated self-signed certificate in AndroidKeyStore with alias: " + keyAlias);
    }
}
