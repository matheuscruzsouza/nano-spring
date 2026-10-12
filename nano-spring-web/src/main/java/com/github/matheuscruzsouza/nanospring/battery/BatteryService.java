package com.github.matheuscruzsouza.nanospring.battery;

import android.content.Context;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Service;

/**
 * Service to inspect battery status and trigger notifications.
 * Can be injected in controllers and services via {@code @Autowired}.
 */
@Service
public class BatteryService {

    @Autowired
    private Context context;

    private int lowBatteryThreshold = BatteryHelper.DEFAULT_LOW_BATTERY_THRESHOLD;

    public BatteryService() {
    }

    public BatteryService(Context context) {
        this.context = context;
    }

    public BatteryService(Context context, int lowBatteryThreshold) {
        this.context = context;
        this.lowBatteryThreshold = lowBatteryThreshold;
    }

    /**
     * Gets the current battery status.
     *
     * @return {@link BatteryStatus} snapshot.
     */
    public BatteryStatus getCurrentStatus() {
        return BatteryHelper.getBatteryStatus(context, lowBatteryThreshold);
    }

    /**
     * Checks if the device is currently experiencing low battery.
     *
     * @return {@code true} if battery is low and not charging.
     */
    public boolean isLowBattery() {
        return getCurrentStatus().isLow();
    }

    /**
     * Sends a low battery notification to the user if the battery level is below threshold.
     *
     * @return {@code true} if notification was sent, {@code false} otherwise.
     */
    public boolean notifyUserIfBatteryLow() {
        BatteryStatus status = getCurrentStatus();
        if (status.isLow()) {
            BatteryNotificationHelper.sendLowBatteryNotification(context, status);
            return true;
        }
        return false;
    }

    /**
     * Sends a custom notification to the user.
     *
     * @param title Title of the notification.
     * @param message Body text.
     */
    public void notifyUser(String title, String message) {
        BatteryNotificationHelper.sendNotification(
                context,
                BatteryNotificationHelper.DEFAULT_BATTERY_CHANNEL_ID,
                BatteryNotificationHelper.DEFAULT_BATTERY_CHANNEL_NAME,
                BatteryNotificationHelper.DEFAULT_BATTERY_NOTIFICATION_ID,
                title,
                message,
                android.R.drawable.stat_sys_warning
        );
    }

    public int getLowBatteryThreshold() {
        return lowBatteryThreshold;
    }

    public void setLowBatteryThreshold(int lowBatteryThreshold) {
        this.lowBatteryThreshold = lowBatteryThreshold;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
