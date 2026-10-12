package com.github.matheuscruzsouza.nanospring.battery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Utility helper to dispatch notifications to the user when battery is running low.
 */
public final class BatteryNotificationHelper {

    public static final String DEFAULT_BATTERY_CHANNEL_ID = "nano_spring_battery_channel";
    public static final String DEFAULT_BATTERY_CHANNEL_NAME = "Nano-Spring Battery Alerts";
    public static final int DEFAULT_BATTERY_NOTIFICATION_ID = 1002;

    private BatteryNotificationHelper() {
        // Utility class
    }

    /**
     * Ensures the notification channel exists on Android 8.0+ (API 26+).
     *
     * @param context Application or Service context.
     * @param channelId Channel ID.
     * @param channelName User-facing channel name.
     */
    public static void createNotificationChannel(Context context, String channelId, String channelName) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Alertas de bateria fraca emitidos pelo Nano-Spring");
                manager.createNotificationChannel(channel);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends a low battery notification to the user using default channel and formatting.
     *
     * @param context Application or Service context.
     * @param status Current {@link BatteryStatus}.
     */
    public static void sendLowBatteryNotification(Context context, BatteryStatus status) {
        if (context == null || status == null) {
            return;
        }

        String title = "Bateria Fraca!";
        String text = "Bateria em " + status.getPercentage() + "%. Conecte o carregador para manter os servicos ativos.";
        sendNotification(context, DEFAULT_BATTERY_CHANNEL_ID, DEFAULT_BATTERY_CHANNEL_NAME,
                DEFAULT_BATTERY_NOTIFICATION_ID, title, text, android.R.drawable.stat_sys_warning);
    }

    /**
     * Sends a customizable notification to the user.
     *
     * @param context Application context.
     * @param channelId Notification channel ID.
     * @param channelName Notification channel name.
     * @param notificationId Notification ID.
     * @param title Title of the notification.
     * @param text Body text of the notification.
     * @param icon Small icon resource ID.
     */
    public static void sendNotification(Context context, String channelId, String channelName,
                                        int notificationId, String title, String text, int icon) {
        if (context == null) {
            return;
        }

        try {
            createNotificationChannel(context, channelId, channelName);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, channelId);
            } else {
                builder = new Notification.Builder(context);
            }

            builder.setContentTitle(title);
            builder.setContentText(text);
            builder.setSmallIcon(icon);
            builder.setAutoCancel(true);

            Notification notification = builder.build();
            manager.notify(notificationId, notification);
        } catch (Exception ignored) {
        }
    }

    /**
     * Cancels the default low battery notification.
     *
     * @param context Application context.
     */
    public static void cancelLowBatteryNotification(Context context) {
        cancelNotification(context, DEFAULT_BATTERY_NOTIFICATION_ID);
    }

    /**
     * Cancels a specific notification by ID.
     *
     * @param context Application context.
     * @param notificationId ID of the notification to dismiss.
     */
    public static void cancelNotification(Context context, int notificationId) {
        if (context == null) {
            return;
        }

        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(notificationId);
            }
        } catch (Exception ignored) {
        }
    }
}
