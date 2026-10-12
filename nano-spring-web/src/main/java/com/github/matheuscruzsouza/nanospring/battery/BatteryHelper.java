package com.github.matheuscruzsouza.nanospring.battery;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Utility helper to query Android battery metrics and low battery status.
 */
public final class BatteryHelper {

    public static final int DEFAULT_LOW_BATTERY_THRESHOLD = 15;

    private BatteryHelper() {
        // Utility class
    }

    /**
     * Retrieves the current device battery status using the default low-battery threshold (15%).
     *
     * @param context Application or Service context.
     * @return {@link BatteryStatus} snapshot or a fallback status if context is unavailable.
     */
    public static BatteryStatus getBatteryStatus(Context context) {
        return getBatteryStatus(context, DEFAULT_LOW_BATTERY_THRESHOLD);
    }

    /**
     * Retrieves the current device battery status.
     *
     * @param context Application or Service context.
     * @param lowThresholdPercent Threshold percentage below which the battery is considered low.
     * @return {@link BatteryStatus} snapshot.
     */
    public static BatteryStatus getBatteryStatus(Context context, int lowThresholdPercent) {
        if (context == null) {
            return new BatteryStatus(-1, -1, -1, false, false, false, -1.0,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN, false, System.currentTimeMillis());
        }

        try {
            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return fromIntent(batteryStatus, lowThresholdPercent);
        } catch (Exception ignored) {
            return new BatteryStatus(-1, -1, -1, false, false, false, -1.0,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN, false, System.currentTimeMillis());
        }
    }

    /**
     * Converts a battery intent to a {@link BatteryStatus} object.
     *
     * @param batteryIntent The intent received from {@link Intent#ACTION_BATTERY_CHANGED}.
     * @param lowThresholdPercent Threshold percentage below which the battery is considered low.
     * @return {@link BatteryStatus} snapshot.
     */
    public static BatteryStatus fromIntent(Intent batteryIntent, int lowThresholdPercent) {
        if (batteryIntent == null) {
            return new BatteryStatus(-1, -1, -1, false, false, false, -1.0,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN, false, System.currentTimeMillis());
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percentage = (level >= 0 && scale > 0) ? Math.round(((float) level / (float) scale) * 100.0f) : -1;

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean pluggedAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        boolean pluggedUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;

        int temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        double tempCelsius = temperature > 0 ? temperature / 10.0 : -1.0;

        int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);

        boolean isLow = (percentage >= 0 && percentage <= lowThresholdPercent && !isCharging);

        return new BatteryStatus(
                level,
                scale,
                percentage,
                isCharging,
                pluggedAc,
                pluggedUsb,
                tempCelsius,
                health,
                isLow,
                System.currentTimeMillis()
        );
    }

    /**
     * Checks if the device battery is currently low and not charging.
     *
     * @param context Application context.
     * @param thresholdPercent Threshold percentage.
     * @return {@code true} if battery is low and not charging.
     */
    public static boolean isBatteryLow(Context context, int thresholdPercent) {
        BatteryStatus status = getBatteryStatus(context, thresholdPercent);
        return status.isLow();
    }
}
