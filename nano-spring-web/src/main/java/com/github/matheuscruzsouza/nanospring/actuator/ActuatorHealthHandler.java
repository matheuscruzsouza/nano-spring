package com.github.matheuscruzsouza.nanospring.actuator;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Build;

import com.github.matheuscruzsouza.nanospring.util.internal.NetworkUtils;
import com.github.matheuscruzsouza.nanospring.server.Server;
import com.github.matheuscruzsouza.nanospring.server.internal.ThreadPoolAsyncRunner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Enterprise Actuator endpoints provider (/actuator/health and /actuator/info).
 * Exposes device metrics, JVM memory, battery stats, SQLite connection status, and thread pool usage.
 */
public class ActuatorHealthHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static NanoHTTPD.Response handleHealth(Server server, Context context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());

        Map<String, Object> components = new LinkedHashMap<>();

        // 1. JVM Memory
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> memDetails = new LinkedHashMap<>();
        memDetails.put("usedBytes", usedMemory);
        memDetails.put("freeBytes", freeMemory);
        memDetails.put("totalBytes", totalMemory);
        memDetails.put("maxBytes", maxMemory);

        Map<String, Object> memoryComp = new LinkedHashMap<>();
        memoryComp.put("status", "UP");
        memoryComp.put("details", memDetails);
        components.put("memory", memoryComp);

        // 2. Database
        if (server != null && server.getDatabase() != null) {
            SQLiteDatabase db = server.getDatabase();
            Map<String, Object> dbDetails = new LinkedHashMap<>();
            dbDetails.put("open", db.isOpen());
            dbDetails.put("readOnly", db.isReadOnly());
            dbDetails.put("path", db.getPath());
            dbDetails.put("version", db.getVersion());
            try {
                dbDetails.put("walEnabled", db.isWriteAheadLoggingEnabled());
            } catch (Exception e) {
                dbDetails.put("walEnabled", false);
            }

            Map<String, Object> dbComp = new LinkedHashMap<>();
            dbComp.put("status", db.isOpen() ? "UP" : "DOWN");
            dbComp.put("details", dbDetails);
            components.put("database", dbComp);
        } else {
            Map<String, Object> dbComp = new LinkedHashMap<>();
            dbComp.put("status", "NOT_CONFIGURED");
            components.put("database", dbComp);
        }

        // 3. Threads / Runner
        if (server != null && server.getThreadPoolAsyncRunner() != null) {
            ThreadPoolAsyncRunner runner = server.getThreadPoolAsyncRunner();
            Map<String, Object> threadDetails = new LinkedHashMap<>();
            threadDetails.put("corePoolSize", runner.getCorePoolSize());
            threadDetails.put("maxPoolSize", runner.getMaxPoolSize());
            threadDetails.put("activeCount", runner.getActiveCount());
            threadDetails.put("queueSize", runner.getQueueSize());
            threadDetails.put("queueCapacity", runner.getQueueCapacity());

            Map<String, Object> threadComp = new LinkedHashMap<>();
            threadComp.put("status", "UP");
            threadComp.put("details", threadDetails);
            components.put("threads", threadComp);
        }

        // 4. Network
        Map<String, Object> netDetails = new LinkedHashMap<>();
        InetAddress ipAddr = NetworkUtils.findLocalIpAddress();
        netDetails.put("ip", ipAddr != null ? ipAddr.getHostAddress() : "unknown");
        netDetails.put("port", server != null ? server.getListeningPort() : 0);

        Map<String, Object> netComp = new LinkedHashMap<>();
        netComp.put("status", "UP");
        netComp.put("details", netDetails);
        components.put("network", netComp);

        // 5. Battery (Android OS)
        if (context != null) {
            try {
                Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    float pct = (level >= 0 && scale > 0) ? ((float) level / (float) scale) * 100.0f : -1.0f;
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;
                    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

                    Map<String, Object> batteryDetails = new LinkedHashMap<>();
                    batteryDetails.put("level", level);
                    batteryDetails.put("percentage", Math.round(pct));
                    batteryDetails.put("charging", isCharging);
                    batteryDetails.put("pluggedAc", chargePlug == BatteryManager.BATTERY_PLUGGED_AC);
                    batteryDetails.put("pluggedUsb", chargePlug == BatteryManager.BATTERY_PLUGGED_USB);
                    batteryDetails.put("temperatureCelsius", temperature > 0 ? temperature / 10.0 : -1);

                    Map<String, Object> batComp = new LinkedHashMap<>();
                    batComp.put("status", "UP");
                    batComp.put("details", batteryDetails);
                    components.put("battery", batComp);
                }
            } catch (Exception ignored) {
            }
        }

        response.put("components", components);
        String json = GSON.toJson(response);
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", json);
    }

    public static NanoHTTPD.Response handleInfo(Context context) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("name", "nano-spring");
        app.put("version", "1.11.1");
        app.put("androidApi", Build.VERSION.SDK_INT);
        app.put("javaVersion", System.getProperty("java.version"));
        app.put("activeProfiles", com.github.matheuscruzsouza.nanospring.server.Environment.getActiveProfiles());
        if (context != null) {
            try {
                app.put("packageName", context.getPackageName());
            } catch (Exception ignored) {
            }
        }
        response.put("app", app);

        String json = GSON.toJson(response);
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", json);
    }

    public static NanoHTTPD.Response handleLogfile(NanoHTTPD.IHTTPSession session) {
        boolean logfileEnabled = Boolean.parseBoolean(
                com.github.matheuscruzsouza.nanospring.server.Environment.getProperty("nano.actuator.logfile.enabled", "true"));
        if (!logfileEnabled) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, "text/plain", "Actuator logfile endpoint is disabled.");
        }

        String linesParam = session != null && session.getParms() != null ? session.getParms().get("lines") : null;
        String logContent;
        if (linesParam != null) {
            try {
                int lines = Integer.parseInt(linesParam.trim());
                logContent = com.github.matheuscruzsouza.nanospring.logging.NanoLogger.readTail(lines);
            } catch (NumberFormatException e) {
                logContent = com.github.matheuscruzsouza.nanospring.logging.NanoLogger.readTail(500);
            }
        } else {
            String allParam = session != null && session.getParms() != null ? session.getParms().get("all") : null;
            if ("true".equalsIgnoreCase(allParam)) {
                logContent = com.github.matheuscruzsouza.nanospring.logging.NanoLogger.readAll();
            } else {
                logContent = com.github.matheuscruzsouza.nanospring.logging.NanoLogger.readTail(500);
            }
        }

        NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/plain; charset=utf-8",
                logContent != null ? logContent : ""
        );
        resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.addHeader("Pragma", "no-cache");
        resp.addHeader("Expires", "0");
        return resp;
    }
}
