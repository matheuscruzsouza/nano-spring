package com.github.matheuscruzsouza.nanospring.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

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
            initializeServer();
        } catch (Exception e) {
            Log.e(TAG, "Error during NanoSpringService startup", e);
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

        startForeground(getNotificationId(), notification);
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

        releaseLocks();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
