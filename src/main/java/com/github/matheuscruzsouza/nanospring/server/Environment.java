package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;

import java.io.InputStream;
import java.util.Properties;

public class Environment {
    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;
        try (InputStream is = context.getAssets().open("application.properties")) {
            properties.load(is);
        } catch (Exception e) {
            System.out.println("No application.properties found in assets, or error loading it.");
        }
        initialized = true;
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static Properties getProperties() {
        return properties;
    }
}
