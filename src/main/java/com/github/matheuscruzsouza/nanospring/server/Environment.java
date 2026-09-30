package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Global application environment configuration.
 * Supports layered properties, active environment profiles (e.g. dev, prod, pos),
 * and dynamic property resolution.
 */
public class Environment {
    private static final Properties properties = new Properties();
    private static boolean initialized = false;
    private static String[] activeProfiles = new String[0];

    public static void init(Context context) {
        if (initialized) return;

        // 1. Load default application.properties
        if (context != null) {
            try (InputStream is = context.getAssets().open("application.properties")) {
                properties.load(is);
            } catch (Exception e) {
                System.out.println("No application.properties found in assets, or error loading it.");
            }
        }

        // 2. Discover and resolve active profiles
        resolveActiveProfiles(context);

        initialized = true;
    }

    private static void resolveActiveProfiles(Context context) {
        String profilesProp = System.getProperty("nano.profiles.active");
        if (profilesProp == null || profilesProp.trim().isEmpty()) {
            profilesProp = System.getenv("NANO_PROFILES_ACTIVE");
        }
        if (profilesProp == null || profilesProp.trim().isEmpty()) {
            profilesProp = properties.getProperty("nano.profiles.active");
        }
        if (profilesProp == null || profilesProp.trim().isEmpty()) {
            profilesProp = properties.getProperty("spring.profiles.active");
        }

        if (profilesProp != null && !profilesProp.trim().isEmpty()) {
            String[] parts = profilesProp.split(",");
            List<String> validProfiles = new ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    validProfiles.add(trimmed);
                }
            }
            activeProfiles = validProfiles.toArray(new String[0]);

            // Load profile-specific property files
            if (context != null) {
                for (String profile : activeProfiles) {
                    String profileFileName = "application-" + profile + ".properties";
                    try (InputStream is = context.getAssets().open(profileFileName)) {
                        Properties profileProps = new Properties();
                        profileProps.load(is);
                        properties.putAll(profileProps);
                        System.out.println("Loaded profile configuration from " + profileFileName);
                    } catch (Exception ignored) {
                        System.out.println("No " + profileFileName + " found for profile '" + profile + "'");
                    }
                }
            }
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    public static void loadProperties(Properties newProps) {
        if (newProps != null) {
            properties.putAll(newProps);
        }
    }

    public static void loadFromStream(InputStream is) {
        if (is != null) {
            try {
                properties.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties getProperties() {
        return properties;
    }

    public static String[] getActiveProfiles() {
        return Arrays.copyOf(activeProfiles, activeProfiles.length);
    }

    public static void setActiveProfiles(String... profiles) {
        if (profiles == null || profiles.length == 0) {
            activeProfiles = new String[0];
            properties.remove("nano.profiles.active");
        } else {
            activeProfiles = Arrays.copyOf(profiles, profiles.length);
            properties.setProperty("nano.profiles.active", String.join(",", activeProfiles));
        }
    }

    public static boolean hasActiveProfile(String profile) {
        if (profile == null) return false;
        for (String p : activeProfiles) {
            if (p.equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void reset() {
        properties.clear();
        activeProfiles = new String[0];
        initialized = false;
    }
}
