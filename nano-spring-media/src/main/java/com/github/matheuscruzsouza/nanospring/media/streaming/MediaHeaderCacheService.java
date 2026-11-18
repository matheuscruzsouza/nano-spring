package com.github.matheuscruzsouza.nanospring.media.streaming;

import com.github.matheuscruzsouza.nanospring.annotation.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight bounded LRU cache for media HTTP headers
 * ({@code Content-Length}, {@code ETag}, {@code Last-Modified}, {@code Content-Type}).
 */
@Service
public class MediaHeaderCacheService {

    private static final int DEFAULT_MAX_ENTRIES = 512;

    private final Map<String, MediaHeaders> cache;

    public MediaHeaderCacheService() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public MediaHeaderCacheService(final int maxEntries) {
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<String, MediaHeaders>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, MediaHeaders> eldest) {
                        return size() > maxEntries;
                    }
                }
        );
    }

    public MediaHeaders get(String key) {
        return cache.get(key);
    }

    public void put(String key, MediaHeaders headers) {
        if (key != null && headers != null) {
            cache.put(key, headers);
        }
    }

    public void invalidate(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public static final class MediaHeaders {

        private final long contentLength;
        private final String eTag;
        private final long lastModified;
        private final String mimeType;

        public MediaHeaders(long contentLength, String eTag, long lastModified, String mimeType) {
            this.contentLength = contentLength;
            this.eTag = eTag;
            this.lastModified = lastModified;
            this.mimeType = mimeType;
        }

        public long getContentLength() {
            return contentLength;
        }

        public String getETag() {
            return eTag;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
