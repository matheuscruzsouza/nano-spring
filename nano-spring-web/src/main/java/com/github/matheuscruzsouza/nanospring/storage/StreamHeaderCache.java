package com.github.matheuscruzsouza.nanospring.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory LRU cache for the leading bytes (headers / metadata containers) of streaming files.
 * Provides fast-start for audio/video media players without repetitive disk I/O on Android flash storage.
 */
public class StreamHeaderCache {

    private final int headerSize;
    private final int maxMemoryBytes;
    private final Map<String, byte[]> cache;
    private int currentMemoryUsage = 0;

    public StreamHeaderCache(int headerSize, int maxMemoryBytes) {
        this.headerSize = headerSize;
        this.maxMemoryBytes = maxMemoryBytes;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    public synchronized byte[] getOrLoad(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        String cacheKey = file.getAbsolutePath() + ":" + file.lastModified() + ":" + file.length();
        byte[] cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        int bytesToRead = (int) Math.min(file.length(), headerSize);
        if (bytesToRead <= 0) {
            return new byte[0];
        }

        byte[] buffer = new byte[bytesToRead];
        try (FileInputStream fis = new FileInputStream(file)) {
            int totalRead = 0;
            while (totalRead < bytesToRead) {
                int read = fis.read(buffer, totalRead, bytesToRead - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }

            if (totalRead < bytesToRead) {
                byte[] trimmed = new byte[totalRead];
                System.arraycopy(buffer, 0, trimmed, 0, totalRead);
                buffer = trimmed;
            }

            evictIfNeeded(buffer.length);
            cache.put(cacheKey, buffer);
            currentMemoryUsage += buffer.length;
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    private void evictIfNeeded(int requiredBytes) {
        Iterator<Map.Entry<String, byte[]>> it = cache.entrySet().iterator();
        while (currentMemoryUsage + requiredBytes > maxMemoryBytes && it.hasNext()) {
            Map.Entry<String, byte[]> entry = it.next();
            currentMemoryUsage -= entry.getValue().length;
            it.remove();
        }
    }

    public synchronized int getCurrentMemoryUsage() {
        return currentMemoryUsage;
    }

    public synchronized int getCachedCount() {
        return cache.size();
    }

    public synchronized void clear() {
        cache.clear();
        currentMemoryUsage = 0;
    }
}
