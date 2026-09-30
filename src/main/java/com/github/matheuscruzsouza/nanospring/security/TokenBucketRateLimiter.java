package com.github.matheuscruzsouza.nanospring.security;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory Token Bucket Rate Limiter per client IP.
 * Protects embedded Android devices from high-volume DoS bursts.
 */
public class TokenBucketRateLimiter {

    private static class Bucket {
        private final double capacity;
        private final double refillTokensPerSecond;
        private double availableTokens;
        private long lastRefillTime;

        public Bucket(double capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.availableTokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedMillis = now - lastRefillTime;
            if (elapsedMillis > 0) {
                double addedTokens = (elapsedMillis / 1000.0) * refillTokensPerSecond;
                availableTokens = Math.min(capacity, availableTokens + addedTokens);
                lastRefillTime = now;
            }
        }

        public synchronized boolean isStale() {
            return (System.currentTimeMillis() - lastRefillTime) > 60000 && availableTokens >= capacity;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final boolean globalEnabled;
    private final int globalRps;
    private final int globalBurst;

    public TokenBucketRateLimiter() {
        this.globalEnabled = Boolean.parseBoolean(Environment.getProperty("nano.ratelimit.enabled", "false"));
        this.globalRps = Integer.parseInt(Environment.getProperty("nano.ratelimit.requests-per-second", "50"));
        this.globalBurst = Integer.parseInt(Environment.getProperty("nano.ratelimit.burst-capacity", "100"));
    }

    public TokenBucketRateLimiter(boolean globalEnabled, int globalRps, int globalBurst) {
        this.globalEnabled = globalEnabled;
        this.globalRps = globalRps;
        this.globalBurst = globalBurst;
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public boolean checkGlobal(String clientIp) {
        if (!globalEnabled) return true;
        return checkLimit("GLOBAL:" + normalizeIp(clientIp), globalBurst, globalRps);
    }

    public boolean checkRoute(String clientIp, String routeKey, int requests, int durationSeconds) {
        int safeDuration = durationSeconds > 0 ? durationSeconds : 1;
        double rps = (double) requests / safeDuration;
        return checkLimit("ROUTE:" + routeKey + ":" + normalizeIp(clientIp), requests, rps);
    }

    public boolean checkLimit(String bucketKey, double capacity, double refillTokensPerSecond) {
        cleanupIfNeeded();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> new Bucket(capacity, refillTokensPerSecond));
        return bucket.tryConsume();
    }

    private String normalizeIp(String clientIp) {
        return clientIp != null && !clientIp.trim().isEmpty() ? clientIp.trim() : "unknown";
    }

    private void cleanupIfNeeded() {
        if (buckets.size() > 500) {
            for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
                if (entry.getValue().isStale()) {
                    buckets.remove(entry.getKey());
                }
            }
        }
    }

    public void clear() {
        buckets.clear();
    }
}
