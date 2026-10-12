package com.github.matheuscruzsouza.nanospring.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.battery.BatteryHelper;
import com.github.matheuscruzsouza.nanospring.battery.BatteryNotificationHelper;
import com.github.matheuscruzsouza.nanospring.battery.BatteryStatus;
import com.github.matheuscruzsouza.nanospring.server.Environment;
import com.github.matheuscruzsouza.nanospring.server.Server;

/**
 * Base Android Foreground Service for running Nano-Spring continuously in the background.
 * Manages notification channels, foreground notifications, partial wake lock,
 * Wi-Fi multicast lock, and server lifecycle.
 */
public abstract class NanoSpringService extends Service {

    private static final String TAG = "NanoSpringService";
    public static final String DEFAULT_CHANNEL_ID = "nano_spring_service_channel";
    public static final String DEFAULT_CHANNEL_NAME = "Nano-Spring Server";
    public static final int DEFAULT_NOTIFICATION_ID = 1001;

    private Server server;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.MulticastLock multicastLock;
    private BroadcastReceiver batteryReceiver;
    private boolean lowBatteryNotified = false;

    /**
     * Package to scan for controllers, services, repositories and interceptors.
     */
    protected abstract String getBasePackage();

    /**
     * Port on which the HTTP server will listen.
     */
    protected int getPort() {
        return Integer.parseInt(Environment.getProperty("server.port", "8080"));
    }

    protected String getChannelId() {
        return DEFAULT_CHANNEL_ID;
    }

    protected String getChannelName() {
        return DEFAULT_CHANNEL_NAME;
    }

    protected String getNotificationTitle() {
        return "Nano-Spring HTTP Server";
    }

    protected String getNotificationText() {
        return "Servidor rodando na porta " + getPort();
    }

    protected int getNotificationIcon() {
        return android.R.drawable.stat_notify_sync;
    }

    protected int getNotificationId() {
        return DEFAULT_NOTIFICATION_ID;
    }

    protected boolean isWakeLockEnabled() {
        return true;
    }

    protected boolean isMulticastLockEnabled() {
        return true;
    }

    /**
     * Determines whether the service should monitor battery state changes and low battery alerts.
     */
    protected boolean isBatteryMonitorEnabled() {
        return Boolean.parseBoolean(Environment.getProperty("nano.battery.monitor.enabled", "false"));
    }

    /**
     * Threshold percentage below which battery is considered low.
     */
    protected int getLowBatteryThreshold() {
        return Integer.parseInt(Environment.getProperty("nano.battery.threshold",
                String.valueOf(BatteryHelper.DEFAULT_LOW_BATTERY_THRESHOLD)));
    }

    /**
     * Determines whether a user notification should be dispatched automatically upon low battery.
     */
    protected boolean isLowBatteryNotificationEnabled() {
        return true;
    }

    /**
     * Hook method invoked when battery drops below the threshold and is not charging.
     *
     * @param status Snapshot of the low battery status.
     */
    protected void onLowBattery(BatteryStatus status) {
        Log.w(TAG, "Low battery warning: " + status.getPercentage() + "%");
        if (isLowBatteryNotificationEnabled()) {
            BatteryNotificationHelper.sendLowBatteryNotification(this, status);
        }
    }

    /**
     * Hook method invoked when battery recovers (starts charging or rises above threshold).
     *
     * @param status Snapshot of the battery status.
     */
    protected void onBatteryRecovered(BatteryStatus status) {
        Log.i(TAG, "Battery level recovered or charging: " + status.getPercentage() + "%");
        if (isLowBatteryNotificationEnabled()) {
            BatteryNotificationHelper.cancelLowBatteryNotification(this);
        }
    }

    /**
     * Foreground service type flag for Android 10+ (API 29+), e.g. {@code ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC}
     * or {@code ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE}.
     * Return 0 to use standard startForeground(id, notification).
     * Note: Renamed to avoid collision with {@code Service#getForegroundServiceType()} which is final in Android SDK.
     */
    protected int getCustomForegroundServiceType() {
        return 0;
    }

    protected Server createServer() {
        return null;
    }

    protected void onServerStarted(Server server) {
    }

    public Server getServer() {
        return server;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Starting NanoSpringService as Foreground Service...");

        try {
            startInForeground();
            acquireLocks();
            checkBatteryOptimizations();
            if (isBatteryMonitorEnabled()) {
                registerBatteryReceiver();
            }
            initializeServer();
        } catch (Exception e) {
            Log.e(TAG, "Error during NanoSpringService startup", e);
        }
    }

    protected void checkBatteryOptimizations() {
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            Log.w(TAG, "Battery optimizations are ACTIVE for " + getPackageName() +
                    ". Android Doze Mode may suspend network or CPU when the screen turns off. " +
                    "Consider requesting exemption via BatteryOptimizationHelper.createRequestIgnoreBatteryOptimizationsIntent().");
        } else {
            Log.d(TAG, "Battery optimizations are IGNORED. Background execution will not be hindered by Doze Mode.");
        }
    }

    protected void startInForeground() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getChannelId(),
                    getChannelName(),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Nano-Spring embedded server foreground notification");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(this, getChannelId());
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle(getNotificationTitle())
                .setContentText(getNotificationText())
                .setSmallIcon(getNotificationIcon())
                .setOngoing(true)
                .build();

        int serviceType = getCustomForegroundServiceType();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && serviceType != 0) {
            startForeground(getNotificationId(), notification, serviceType);
        } else {
            startForeground(getNotificationId(), notification);
        }
    }

    protected void acquireLocks() {
        if (isWakeLockEnabled()) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nano-spring:server-wakelock");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire();
                    Log.d(TAG, "PowerManager.WakeLock acquired.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not acquire WakeLock: " + e.getMessage());
            }
        }

        if (isMulticastLockEnabled()) {
            try {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    multicastLock = wm.createMulticastLock("nano-spring:service-multicast");
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();
                    Log.d(TAG, "WifiManager.MulticastLock acquired.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not acquire MulticastLock: " + e.getMessage());
            }
        }
    }

    protected void releaseLocks() {
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.d(TAG, "PowerManager.WakeLock released.");
                }
            } catch (Exception ignored) {
            } finally {
                wakeLock = null;
            }
        }

        if (multicastLock != null) {
            try {
                if (multicastLock.isHeld()) {
                    multicastLock.release();
                    Log.d(TAG, "WifiManager.MulticastLock released.");
                }
            } catch (Exception ignored) {
            } finally {
                multicastLock = null;
            }
        }
    }

    protected void initializeServer() {
        Server customServer = createServer();
        if (customServer != null) {
            this.server = customServer;
        } else {
            this.server = new Server(this, getPort(), getBasePackage());
        }
        onServerStarted(this.server);
        Log.i(TAG, "Nano-Spring server initialized on port " + getPort());
    }

    protected void registerBatteryReceiver() {
        if (batteryReceiver != null) {
            return;
        }

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                BatteryStatus status = BatteryHelper.fromIntent(intent, getLowBatteryThreshold());
                if (status.isLow()) {
                    if (!lowBatteryNotified) {
                        lowBatteryNotified = true;
                        onLowBattery(status);
                    }
                } else if (lowBatteryNotified && (status.isCharging() || status.getPercentage() > getLowBatteryThreshold())) {
                    lowBatteryNotified = false;
                    onBatteryRecovered(status);
                }
            }
        };

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_BATTERY_LOW);
            filter.addAction(Intent.ACTION_BATTERY_OKAY);
            registerReceiver(batteryReceiver, filter);
            Log.d(TAG, "Battery broadcast receiver registered.");
        } catch (Exception e) {
            Log.w(TAG, "Could not register battery receiver: " + e.getMessage());
        }
    }

    protected void unregisterBatteryReceiver() {
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
                Log.d(TAG, "Battery broadcast receiver unregistered.");
            } catch (Exception ignored) {
            } finally {
                batteryReceiver = null;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroying NanoSpringService...");
        if (server != null) {
            try {
                server.stop();
                Log.i(TAG, "Server stopped gracefully.");
            } catch (Exception e) {
                Log.w(TAG, "Error stopping server: " + e.getMessage());
            } finally {
                server = null;
            }
        }

        unregisterBatteryReceiver();
        releaseLocks();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
