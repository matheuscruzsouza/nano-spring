package com.github.matheuscruzsouza.nanospring.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenBucketRateLimiterTest {

    @Test
    public void testRouteRateLimitExceeded() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 50, 100);
        String ip = "192.168.1.100";
        String routeKey = "UserController#getUsers";

        // Limit 3 requests in 1 second
        assertTrue("Request 1 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));
        assertTrue("Request 2 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));
        assertTrue("Request 3 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));

        // 4th request must be rejected
        assertFalse("Request 4 should be rejected", limiter.checkRoute(ip, routeKey, 3, 1));
    }

    @Test
    public void testDifferentIpsHaveIndependentBuckets() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 50, 100);
        String routeKey = "OrderController#create";

        assertTrue(limiter.checkRoute("10.0.0.1", routeKey, 1, 1));
        assertFalse(limiter.checkRoute("10.0.0.1", routeKey, 1, 1));

        // Different IP should still have its own quota
        assertTrue(limiter.checkRoute("10.0.0.2", routeKey, 1, 1));
    }

    @Test
    public void testGlobalRateLimiterEnabled() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(true, 2, 2);
        String ip = "127.0.0.1";

        assertTrue(limiter.checkGlobal(ip));
        assertTrue(limiter.checkGlobal(ip));
        assertFalse("3rd global request should exceed burst capacity of 2", limiter.checkGlobal(ip));
    }

    @Test
    public void testGlobalRateLimiterDisabledAllowsAll() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 1, 1);
        String ip = "127.0.0.1";

        for (int i = 0; i < 10; i++) {
            assertTrue("Disabled global rate limiter should allow all requests", limiter.checkGlobal(ip));
        }
    }

    @Test
    public void testPartiallyConsumedBucketIsStaleAfterInactivity() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(true, 50, 100);
        limiter.checkGlobal("192.168.1.1");

        TokenBucketRateLimiter.Bucket bucket = limiter.getBuckets().get("GLOBAL:192.168.1.1");
        assertFalse("Bucket should not be stale immediately after use", bucket.isStale());

        // Simulate inactivity beyond 60s
        bucket.setLastAccessTimeForTest(System.currentTimeMillis() - 61000);
        assertTrue("Bucket should be stale after 60s of inactivity even if partially consumed", bucket.isStale());
    }

    @Test
    public void testCleanupRemovesStaleBucketsWhenThresholdExceeded() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(true, 50, 100);

        for (int i = 0; i < 501; i++) {
            limiter.checkGlobal("10.0.0." + i);
        }

        // Set the first 100 buckets as inactive/stale
        long staleTime = System.currentTimeMillis() - 65000;
        for (int i = 0; i < 100; i++) {
            TokenBucketRateLimiter.Bucket b = limiter.getBuckets().get("GLOBAL:10.0.0." + i);
            if (b != null) {
                b.setLastAccessTimeForTest(staleTime);
            }
        }

        limiter.cleanupIfNeeded();

        // Stale buckets must be removed
        for (int i = 0; i < 100; i++) {
            assertFalse("Stale bucket should have been cleaned up", limiter.getBuckets().containsKey("GLOBAL:10.0.0." + i));
        }
        // Active buckets must remain
        assertTrue("Active bucket should be retained", limiter.getBuckets().containsKey("GLOBAL:10.0.0.500"));
    }
}
