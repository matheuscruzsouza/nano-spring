package com.github.matheuscruzsouza.nanospring.logging;

import android.content.Context;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.Environment;
import com.github.matheuscruzsouza.nanospring.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Enterprise thread-safe rotating file logger for embedded Android systems (POS, totems, hubs).
 * Prevents device storage exhaustion via size caps and generational file rotation.
 */
public class RotatingFileLogger {

    private static RotatingFileLogger instance;

    public enum Level {
        TRACE(1), DEBUG(2), INFO(3), WARN(4), ERROR(5);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public boolean isEnabled(Level threshold) {
            return this.priority >= threshold.priority;
        }

        public static Level fromString(String val, Level defaultLevel) {
            if (val == null) return defaultLevel;
            try {
                return Level.valueOf(val.trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return defaultLevel;
            }
        }
    }

    private final Object lock = new Object();
    private File logFile;
    private long maxSizeBytes = 2 * 1024 * 1024; // 2 MB default
    private int maxHistory = 3; // 3 backup files default: .1, .2, .3
    private Level currentLevel = Level.INFO;
    private boolean enabled = true;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private final Deque<String> memoryBuffer = new ArrayDeque<>(1000);
    private final int memoryBufferSize = 1000;

    public RotatingFileLogger() {
        configureFromEnvironment();
    }

    public static synchronized RotatingFileLogger getInstance() {
        if (instance == null) {
            instance = new RotatingFileLogger();
        }
        return instance;
    }

    public synchronized void configureFromEnvironment() {
        this.enabled = Boolean.parseBoolean(Environment.getProperty("nano.logging.enabled", "true"));
        String lvlStr = Environment.getProperty("nano.logging.level", "INFO");
        this.currentLevel = Level.fromString(lvlStr, Level.INFO);

        String sizeMbStr = Environment.getProperty("nano.logging.max-size-mb", null);
        if (sizeMbStr != null) {
            try {
                this.maxSizeBytes = Long.parseLong(sizeMbStr.trim()) * 1024 * 1024;
            } catch (Exception ignored) {}
        } else {
            String sizeKbStr = Environment.getProperty("nano.logging.max-size-kb", null);
            if (sizeKbStr != null) {
                try {
                    this.maxSizeBytes = Long.parseLong(sizeKbStr.trim()) * 1024;
                } catch (Exception ignored) {}
            }
        }

        String historyStr = Environment.getProperty("nano.logging.max-history", "3");
        try {
            this.maxHistory = Integer.parseInt(historyStr.trim());
        } catch (Exception ignored) {}

        resolveLogFile();
    }

    private void resolveLogFile() {
        String explicitPath = Environment.getProperty("nano.logging.file", null);
        if (explicitPath != null && !explicitPath.trim().isEmpty()) {
            this.logFile = new File(explicitPath.trim());
        } else {
            Context ctx = Server.getContext();
            if (ctx != null && ctx.getFilesDir() != null) {
                File dir = new File(ctx.getFilesDir(), "logs");
                if (!dir.exists()) dir.mkdirs();
                this.logFile = new File(dir, "nano-spring.log");
            } else {
                File tmpDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "nano-spring-logs");
                if (!tmpDir.exists()) tmpDir.mkdirs();
                this.logFile = new File(tmpDir, "nano-spring.log");
            }
        }
    }

    public void setLogFile(File logFile) {
        synchronized (lock) {
            this.logFile = logFile;
        }
    }

    public File getLogFile() {
        synchronized (lock) {
            return logFile;
        }
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        synchronized (lock) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }

    public void setMaxHistory(int maxHistory) {
        synchronized (lock) {
            this.maxHistory = maxHistory;
        }
    }

    public void setLevel(Level level) {
        synchronized (lock) {
            this.currentLevel = level;
        }
    }

    public Level getLevel() {
        synchronized (lock) {
            return currentLevel;
        }
    }

    public void setEnabled(boolean enabled) {
        synchronized (lock) {
            this.enabled = enabled;
        }
    }

    public boolean isEnabled() {
        synchronized (lock) {
            return enabled;
        }
    }

    public void log(Level level, String tag, String message, Throwable throwable) {
        if (!enabled || !level.isEnabled(currentLevel)) {
            return;
        }

        String timestamp;
        synchronized (dateFormat) {
            timestamp = dateFormat.format(new Date());
        }

        String threadName = Thread.currentThread().getName();
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(" [").append(threadName).append("] ")
                .append(level.name()).append(" ")
                .append(tag != null ? tag : "APP").append(" - ")
                .append(message != null ? message : "");

        if (throwable != null) {
            sb.append("\n");
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw.toString());
        }

        String logLine = sb.toString();

        synchronized (lock) {
            // 1. In-memory buffer
            if (memoryBuffer.size() >= memoryBufferSize) {
                memoryBuffer.pollFirst();
            }
            memoryBuffer.addLast(logLine);

            // 2. Rotating file
            writeToFile(logLine);
        }
    }

    private void writeToFile(String line) {
        if (logFile == null) {
            resolveLogFile();
        }
        if (logFile == null) return;

        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Check if rotation is needed
            if (logFile.exists() && logFile.length() + line.length() >= maxSizeBytes) {
                rotateFiles();
            }

            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 PrintWriter pw = new PrintWriter(osw)) {
                pw.println(line);
            }
        } catch (IOException e) {
            System.err.println("RotatingFileLogger failed to write log: " + e.getMessage());
        }
    }

    private void rotateFiles() {
        if (maxHistory <= 0) {
            logFile.delete();
            return;
        }

        // Delete oldest backup: e.g. nano-spring.log.3
        File oldest = new File(logFile.getAbsolutePath() + "." + maxHistory);
        if (oldest.exists()) {
            oldest.delete();
        }

        // Shift existing backups: .2 -> .3, .1 -> .2
        for (int i = maxHistory - 1; i >= 1; i--) {
            File src = new File(logFile.getAbsolutePath() + "." + i);
            if (src.exists()) {
                File dest = new File(logFile.getAbsolutePath() + "." + (i + 1));
                src.renameTo(dest);
            }
        }

        // Rename current log file to .1
        File backup1 = new File(logFile.getAbsolutePath() + ".1");
        logFile.renameTo(backup1);
    }

    public List<String> getRecentMemoryLogs(int maxLines) {
        synchronized (lock) {
            List<String> list = new ArrayList<>(memoryBuffer);
            if (maxLines <= 0 || list.size() <= maxLines) {
                return list;
            }
            return list.subList(list.size() - maxLines, list.size());
        }
    }

    public String readTail(int lines) {
        synchronized (lock) {
            if (logFile == null || !logFile.exists()) {
                List<String> mem = getRecentMemoryLogs(lines);
                return String.join("\n", mem);
            }

            List<String> tail = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                Deque<String> ring = new ArrayDeque<>(lines > 0 ? lines : 500);
                int limit = lines > 0 ? lines : 500;
                String current;
                while ((current = reader.readLine()) != null) {
                    if (ring.size() >= limit) {
                        ring.pollFirst();
                    }
                    ring.addLast(current);
                }
                tail.addAll(ring);
            } catch (IOException e) {
                return "Error reading logfile: " + e.getMessage();
            }
            return String.join("\n", tail);
        }
    }

    public String readAll() {
        synchronized (lock) {
            if (logFile == null || !logFile.exists()) {
                return String.join("\n", memoryBuffer);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                return "Error reading logfile: " + e.getMessage();
            }
            return sb.toString();
        }
    }

    public void clear() {
        synchronized (lock) {
            memoryBuffer.clear();
            if (logFile != null && logFile.exists()) {
                logFile.delete();
            }
        }
    }
}
