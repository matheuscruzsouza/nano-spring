package com.github.matheuscruzsouza.nanospring.logging;

import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * High-performance facade for framework and application logging in nano-spring.
 * Synchronously or asynchronously writes to Android logcat and persistent rolling disk storage.
 */
public class NanoLogger {

    private static final RotatingFileLogger fileLogger = RotatingFileLogger.getInstance();

    public static void trace(String tag, String message) {
        log(RotatingFileLogger.Level.TRACE, tag, message, null);
    }

    public static void debug(String tag, String message) {
        log(RotatingFileLogger.Level.DEBUG, tag, message, null);
    }

    public static void info(String tag, String message) {
        log(RotatingFileLogger.Level.INFO, tag, message, null);
    }

    public static void warn(String tag, String message) {
        log(RotatingFileLogger.Level.WARN, tag, message, null);
    }

    public static void warn(String tag, String message, Throwable throwable) {
        log(RotatingFileLogger.Level.WARN, tag, message, throwable);
    }

    public static void error(String tag, String message) {
        log(RotatingFileLogger.Level.ERROR, tag, message, null);
    }

    public static void error(String tag, String message, Throwable throwable) {
        log(RotatingFileLogger.Level.ERROR, tag, message, throwable);
    }

    private static void log(RotatingFileLogger.Level level, String tag, String message, Throwable th) {
        // 1. Android Logcat (safe wrapper for plain JVM test environments)
        try {
            switch (level) {
                case TRACE:
                case DEBUG:
                    Log.d(tag, message);
                    break;
                case INFO:
                    Log.i(tag, message);
                    break;
                case WARN:
                    if (th != null) Log.w(tag, message, th);
                    else Log.w(tag, message);
                    break;
                case ERROR:
                    if (th != null) Log.e(tag, message, th);
                    else Log.e(tag, message);
                    break;
            }
        } catch (Throwable ignored) {
            // JVM environment without Android mock
        }

        // 2. Persistent Rolling File Logger
        fileLogger.log(level, tag, message, th);
    }

    public static RotatingFileLogger getFileLogger() {
        return fileLogger;
    }

    public static String readTail(int lines) {
        return fileLogger.readTail(lines);
    }

    public static String readAll() {
        return fileLogger.readAll();
    }

    public static List<String> getRecentMemoryLogs(int maxLines) {
        return fileLogger.getRecentMemoryLogs(maxLines);
    }

    public static File getLogFile() {
        return fileLogger.getLogFile();
    }
}
