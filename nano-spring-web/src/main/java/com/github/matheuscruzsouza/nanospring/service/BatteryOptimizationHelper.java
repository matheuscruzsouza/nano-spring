package com.github.matheuscruzsouza.nanospring.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * Utility helper to manage Android battery optimizations (Doze Mode and App Standby).
 * Helps prevent the operating system from suspending or killing background HTTP services
 * when the screen is turned off.
 */
public final class BatteryOptimizationHelper {

    private BatteryOptimizationHelper() {
        // Utility class
    }

    /**
     * Checks if the app is currently whitelisted from battery optimizations (Doze Mode).
     *
     * @param context Application or Service context.
     * @return {@code true} if battery optimizations are ignored, {@code false} otherwise.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    return pm.isIgnoringBatteryOptimizations(context.getPackageName());
                }
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    /**
     * Creates an {@link Intent} to request the user to whitelist the app from battery optimizations.
     * Note: Requires {@code android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS} in the host app's manifest.
     *
     * @param packageName The package name of the application.
     * @return Intent ready to be used with {@code startActivity()}.
     */
    public static Intent createRequestIgnoreBatteryOptimizationsIntent(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            return intent;
        }
        return createBatteryOptimizationSettingsIntent();
    }

    /**
     * Creates an {@link Intent} to open the system battery optimization settings screen.
     * Can be used as a safer fallback when requesting direct exemption is not allowed by store policies.
     *
     * @return Intent to open battery optimization settings.
     */
    public static Intent createBatteryOptimizationSettingsIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        }
        return new Intent(Settings.ACTION_SETTINGS);
    }
}
